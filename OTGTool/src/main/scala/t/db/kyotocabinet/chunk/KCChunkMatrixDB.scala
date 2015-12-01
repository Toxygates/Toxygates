package t.db.kyotocabinet.chunk

import t.db.PExprValue
import t.db.MatrixDB
import t.db.ExprValue
import t.db.ProbeMap
import t.db.Sample
import t.db.MatrixContext
import java.nio.ByteBuffer
import t.db.ExtMatrixDB
import t.global.KCDBRegistry
import kyotocabinet.DB
import t.db.kyotocabinet.KyotoCabinetDB

/**
 * A vector-chunk of adjacent values in a vector in the matrix.
 * @param sample the sample this chunk belongs to.
 * @param start the probe where this chunk's contiguous range begins.
 */
case class VectorChunk[E <: ExprValue] (sample: Int, start: Int, xs: Seq[(Int, E)]) {

  assert (start % CHUNKSIZE == 0)

  /**
   * The probes in this chunk, in order.
   */
  def probes: Seq[Int] =
    xs.map(_._1)

  /**
   * Write the given probe's value, or create it if it doesn't exist.
   */
  def insert(p: Int, x: E): VectorChunk[E] = {
    val nxs = (xs.takeWhile(_._1 < p) :+ (p, x)) ++ xs.dropWhile(_._1 <= p)

    assert(p >= start)
    assert(p < start + CHUNKSIZE)
    assert(nxs.size <= CHUNKSIZE)
    VectorChunk(sample, start, nxs)
  }

  /**
   * Remove the given probe's value.
   */
  def remove(p: Int): VectorChunk[E] = {
    val nxs = xs.takeWhile(_._1 < p) ++ xs.dropWhile(_._1 <= p)
    VectorChunk(sample, start, nxs)
  }
}

object KCChunkMatrixDB {
  val CHUNK_PREFIX = "kcchunk:"

  def removePrefix(file: String) = file.split(CHUNK_PREFIX)(1)

  def apply(file: String, writeMode: Boolean)(implicit context: MatrixContext) = {
    val db = KCDBRegistry.get(file, writeMode)
    db match {
      case Some(d) =>
        new KCChunkMatrixDB(d)
      case None => throw new Exception("Unable to get DB")
    }
  }
}

/**
 * Chunked matrix DB.
 * Key size: 8 bytes (sample + probe)
 * Value size: 22b * chunksize (2816b at 128 probe chunks)
 * Expected number of records: 5-10 million
 */
class KCChunkMatrixDB(db: DB)(implicit mc: MatrixContext)
  extends KyotoCabinetDB(db) with ExtMatrixDB {

  type V = VectorChunk[PExprValue]

  protected def formKey(vc: V): Array[Byte] =
    formKey(vc.sample, vc.start)

  protected def formKey(s: Int, p: Int): Array[Byte] = {
    val r = ByteBuffer.allocate(8)
    r.putInt(s)
    r.putInt(p)
    r.array()
  }

  protected def extractKey(data: Array[Byte]): (Int, Int) = {
    val b = ByteBuffer.wrap(data)
    (b.getInt, b.getInt)
  }

  private val CHUNKVALSIZE = (4 + (8 + 8 + 2))

  protected def formValue(vc: V): Array[Byte] = {
    val r = ByteBuffer.allocate(vc.xs.size * CHUNKVALSIZE)
    for (x <- vc.xs) {
      r.putInt(x._1) //probe
      r.putDouble(x._2.value)
      r.putDouble(x._2.p)
      r.putChar(x._2.call)
    }
    r.array()
  }

  protected def extractValue(sample: Int, start: Int,
      data: Array[Byte]): V = {
    val b = ByteBuffer.wrap(data)
    var r = List[(Int, PExprValue)]()

    while(b.hasRemaining()) {
      val pr = b.getInt
      val x = b.getDouble
      val p = b.getDouble
      val c = b.getChar
      r ::= (pr, PExprValue(x, p, c))
    }
    VectorChunk(sample, start, r.reverse)
  }

  /**
   * Read all chunk keys as sample,probe-pairs
   */
  private def allChunks: Iterable[(Int, Int)] = {
    val cur = db.cursor()
    var continue = cur.jump()
    var r = Vector[(Int, Int)]()
    while (continue) {
      val key = cur.get_key(true)
      if (key != null) {
        val ex = extractKey(key)
        r :+= (ex._1, ex._2)
      } else {
        continue = false
      }
    }
    r
  }

  private def chunkStartFor(p: Int): Int = {
    (p / CHUNKSIZE) * CHUNKSIZE
  }

  /**
   * Find the chunk that contains the given sample/probe pair, or create a
   * blank chunk if it doesn't exist.
   */
  private def findOrCreateChunk(sample: Int, probe: Int): V = {
    val start = chunkStartFor(probe)
    val key = formKey(sample, start)
    val v = db.get(key)
    if (v == null) {
      new VectorChunk[PExprValue](sample, start, Seq())
    } else {
      extractValue(sample, start, v)
    }
  }

  /**
   * Write the (possibly new) chunk into the database, overwriting it
   * if it already existed. If there are no values in it, it will be
   * removed.
   */
  private def updateChunk(v: V): Unit = {
    val key = formKey(v)

    if (v.xs.size == 0) {
      db.remove(key)
    } else {
      val value = formValue(v)
      if (!db.set(key, value)) {
        throw new Exception("Failed to write value")
      }
    }
  }

  /**
   * Delete a chunk by key.
   */
  private def deleteChunk(sample: Int, probe: Int): Unit = {
    val key = new VectorChunk[PExprValue](sample, probe, Seq())
    updateChunk(key)
  }

  def allSamples: Iterable[Sample] =
    allChunks.map(x => Sample(x._1))

  implicit val probeMap = mc.probeMap

  def sortSamples(xs: Iterable[Sample]): Seq[Sample] =
    xs.toSeq.sortBy(_.dbCode)

  def valuesForProbe(probe: Int, xs: Seq[Sample]): Iterable[(Sample, PExprValue)] = {
    val probeName = probeMap.unpack(probe)
    val cs = xs.map(x => findOrCreateChunk(x.dbCode, probe))
    for (c <- cs; x <- c.xs; if (x._1 == probe))
      yield (Sample(c.sample), x._2.copy(probe = probeName))
  }

  //probes must be sorted in an order consistent with the chunkDB.
  def valuesInSample(x: Sample, probes: Iterable[Int]): Iterable[PExprValue] = {
    //The chunk system guarantees that values will be read in order.
    //We exploit the ordering here when checking for missing values.

//    assert(probes.toSeq.sorted == probes.toSeq)

    val keys = probes.map(p => chunkStartFor(p)).toSeq.distinct
    val chunks = keys.map(k => findOrCreateChunk(x.dbCode, k))

//    assert(chunks.sortBy(_.start) == chunks)
//    for (c <- chunks) {
//      if(c.xs.sortBy(_._1) != c.xs) {
//        println(c.xs)
//        assert(false)
//      }
//    }

    val pit = probes.iterator.buffered
    val vit = (chunks.flatMap(_.xs)).iterator.buffered
    var r = Vector[PExprValue]()

    while (vit.hasNext && pit.hasNext) {
      if (vit.head._1 > pit.head) {
        //missing value - do not advance vit
        r :+= emptyValue(probeMap.unpack(pit.next))
      } else if (vit.head._1 < pit.head) {
        //non-requested value
        vit.next
      } else {
        r :+= vit.next._2.copy(probe = probeMap.unpack(pit.next))
      }
    }
    while (pit.hasNext) {
        r :+= emptyValue(probeMap.unpack(pit.next))
    }
    r
  }

  def deleteSample(s: Sample): Unit = {
    val x = s.dbCode
    val ds = allChunks.filter(_._1 == x)
    for (d <- ds) {
      deleteChunk(d._1, d._2)
    }
  }

  def write(s: Sample, probe: Int, e: PExprValue): Unit = synchronized {
    val c = findOrCreateChunk(s.dbCode, probe)
    val u = c.insert(probe, e)
    updateChunk(u)
  }
}