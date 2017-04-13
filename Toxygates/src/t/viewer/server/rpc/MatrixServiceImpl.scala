/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.server.rpc

import java.util.ArrayList
import java.util.{ List => JList }
import java.util.logging.Logger
import org.apache.commons.lang.StringUtils
import javax.annotation.Nullable
import t.Context
import t.common.server.ScalaUtils
import t.clustering.server.RClustering
import t.common.shared.ValueType
import t.common.shared.sample.ExpressionRow
import t.common.shared.sample.Group
import t.common.shared.sample.Sample
import t.clustering.shared.Algorithm
import t.db.MatrixContext
import t.platform.OrthologMapping
import t.platform.Probe
import t.sparql.makeRich
import t.viewer.client.rpc.MatrixService
import t.viewer.server.CSVHelper
import t.viewer.server.Configuration
import t.viewer.server.ExprMatrix
import t.viewer.server.Feedback
import t.viewer.server.ManagedMatrix
import t.viewer.server.MatrixController
import t.viewer.shared.ColumnFilter
import t.viewer.shared.ManagedMatrixInfo
import t.viewer.shared.Synthetic
import t.viewer.shared.table.SortKey
import t.viewer.shared.NoDataLoadedException
import t.viewer.shared.FullMatrix
import t.clustering.client.ClusteringService
import t.clustering.server.RandomData

object MatrixServiceImpl {

  private var orthologs: Option[Iterable[OrthologMapping]] = None

  def getOrthologs(c: Context) = synchronized {
    if (orthologs == None) {
      val probes = c.probes
      orthologs = Some(probes.orthologMappings)
    }
    orthologs.get
  }
}

/**
 * This servlet is responsible for obtaining and manipulating microarray data.
 */
abstract class MatrixServiceImpl extends TServiceServlet with MatrixService {
  import scala.collection.JavaConversions._
  import t.viewer.server.Conversions._
  import ScalaUtils._
  import MatrixServiceImpl._

  protected implicit var mcontext: MatrixContext = _
  private def probes = context.probes
  private var config: Configuration = _
  private val logger = Logger.getLogger("MatrixService")
  private var userDir: String = null

  // Useful for testing
  override def localInit(config: Configuration) {
    super.localInit(config)
    this.config = config
    this.userDir = this.getServletContext.getRealPath("/WEB-INF/")
    mcontext = context.matrix
  }

  protected class MatrixState {
    var _controller: Option[MatrixController] = None
    def controller =
      _controller.getOrElse(throw new NoDataLoadedException)

    def matrix: ManagedMatrix =
      controller.managedMatrix

    def matrixOption = _controller.map(_.managedMatrix)

    def needsReload(groups: Iterable[Group], typ: ValueType): Boolean = {
      _controller == None ||
        controller.groups.toSet != groups.toSet ||
        controller.typ != typ
    }
  }

  def getSessionData(): MatrixState = {
    val session = getThreadLocalRequest.getSession
    val r = Option(session.getAttribute("matrix").
      asInstanceOf[MatrixState])
    r match {
      case Some(ms) => ms
      case None =>
        val r = new MatrixState()
        session.setAttribute("matrix", r)
        r
    }
  }

  def setSessionData(m: MatrixState) =
    getThreadLocalRequest().getSession().setAttribute("matrix", m)

  def loadMatrix(groups: JList[Group], probes: Array[String],
    typ: ValueType): ManagedMatrixInfo = {
    getSessionData._controller =
      Some(MatrixController(context, () => getOrthologs(context),
          groups, probes, typ, false))
    getSessionData.matrix.info
  }

  @throws(classOf[NoDataLoadedException])
  def selectProbes(@Nullable probes: Array[String]): ManagedMatrixInfo = {
    val prs = Option(probes).getOrElse(Array()).toSeq
    getSessionData.controller.selectProbes(prs).info
  }

  @throws(classOf[NoDataLoadedException])
  def setColumnFilter(column: Int, f: ColumnFilter): ManagedMatrixInfo = {
    val mm = getSessionData.matrix

    println(s"Filter for column $column: $f")
    mm.setFilter(column, f)
    mm.info
  }

  @throws(classOf[NoDataLoadedException])
  def matrixRows(offset: Int, size: Int, sortKey: SortKey,
    ascending: Boolean): JList[ExpressionRow] = {
    val mm =
      getSessionData.controller.applySorting(sortKey, ascending)

    val grouped = mm.current.asRows.drop(offset).take(size)

    val rowNames = grouped.map(_.getProbe)
    val rawData = mm.finalTransform(mm.rawUngrouped.selectNamedRows(rowNames)).data

    for ((gr, rr) <- grouped zip rawData;
      (gv, i) <- gr.getValues.zipWithIndex) {
      val tooltip = if (mm.info.isPValueColumn(i)) {
        "p-value (t-test treated against control)"
      } else {
        val basis = mm.baseColumns(i)
        val rawRow = basis.map(i => rr(i))
        ManagedMatrix.makeTooltip(rawRow)
      }
      gv.setTooltip(tooltip)
    }

    new ArrayList[ExpressionRow](insertAnnotations(grouped))
  }

  private def insertAnnotations(rows: Seq[ExpressionRow]): Seq[ExpressionRow] = {
    val rl = getSessionData.controller.rowLabels(schema)
    rl.insertAnnotations(rows)
  }

  def getFullData(gs: JList[Group], rprobes: Array[String],
    withSymbols: Boolean, typ: ValueType): FullMatrix = {
    val sgs = Vector() ++ gs
    val controller = MatrixController(context, () => getOrthologs(context),
        gs, rprobes, typ, true)
    val mm = controller.managedMatrix

    val raw = if (sgs.size == 1) {
      //break out each individual sample if it's only one group
      val ss = sgs(0).getSamples().map(_.id)
      mm.finalTransform(mm.rawUngrouped.selectNamedColumns(ss)).asRows
    } else {
      val ss = sgs.map(_.getName)
      mm.current.selectNamedColumns(ss).asRows
    }

    val rows = if (withSymbols) {
      insertAnnotations(raw)
    } else {
      val ps = raw.flatMap(or => or.getAtomicProbes.map(Probe(_)))
      val ats = probes.withAttributes(ps)
      val giMap = Map() ++ ats.map(x =>
        (x.identifier -> x.genes.map(_.identifier).toArray))

      //Only insert geneIDs.
      //TODO: some clients need neither "symbols"/annotations nor geneIds
      raw.map(or => {
        new ExpressionRow(or.getProbe, or.getAtomicProbes, or.getAtomicProbeTitles,
          or.getAtomicProbes.flatMap(p => giMap(p)),
          or.getGeneSyms, or.getValues)
      })
    }
    new FullMatrix(mm.info, new ArrayList[ExpressionRow](rows))
  }

  @throws(classOf[NoDataLoadedException])
  def addTwoGroupTest(test: Synthetic.TwoGroupSynthetic): ManagedMatrixInfo = {
    val current = getSessionData.matrix
    current.addSynthetic(test)
    current.info
  }

  @throws(classOf[NoDataLoadedException])
  def removeTwoGroupTests(): ManagedMatrixInfo = {
    val current = getSessionData.matrix
    current.removeSynthetics()
    current.info
  }

  @throws(classOf[NoDataLoadedException])
  def prepareCSVDownload(individualSamples: Boolean): String = {
    val mm = getSessionData.matrix
    var mat = if (individualSamples &&
      mm.rawUngrouped != null && mm.current != null) {
      //Individual samples
      val info = mm.info
      val keys = mm.current.rowKeys.toSeq
      val ug = mm.finalTransform(mm.rawUngrouped.selectNamedRows(keys))
      val parts = (0 until info.numDataColumns).map(g => {
        if (!info.isPValueColumn(g)) {
          //Sample data.
          //Help the user by renaming the columns.
          //Prefix sample IDs by group IDs.

          //Here we get both treated and control samples from cg, but
          //except for single unit columns in the normalized intensity case,
          // only treated will be present in ug.
          val ids = info.samples(g).map(_.id)
          val sel = ug.selectNamedColumns(ids)
          val newNames = Map() ++ sel.columnMap.map(x => (info.columnName(g) + ":" + x._1 -> x._2))
          sel.copyWith(sel.data, sel.rowMap, newNames)
        } else {
          //p-value column, present as it is
          mm.current.selectNamedColumns(List(info.columnName(g)))
        }
      })

      parts.reduce(_ adjoinRight _)
    } else {
      //Grouped
      mm.current
    }

    val colNames = mat.sortedColumnMap.map(_._1)
    val rows = mat.asRows
    //TODO move into RowLabels if possible
    val rowNames = rows.map(_.getAtomicProbes.mkString("/"))

    //May be slow!
    val gis = probes.allGeneIds.mapInnerValues(_.identifier)
    val atomics = rows.map(_.getAtomicProbes())
    val geneIds = atomics.map(row =>
      row.flatMap(at => gis.getOrElse(Probe(at), Seq.empty))).map(_.distinct.mkString(" "))

    val aux = List((("Gene"), geneIds))
    CSVHelper.writeCSV("toxygates", config.csvDirectory, config.csvUrlBase,
      aux ++ csvAuxColumns(mat),
      rowNames, colNames,
      mat.data.map(_.map(_.getValue)))
  }

  protected def csvAuxColumns(mat: ExprMatrix): Seq[(String, Seq[String])] = Seq()

  @throws(classOf[NoDataLoadedException])
  def getGenes(limit: Int): Array[String] = {
    val mm = getSessionData().matrix

    var rowNames = mm.current.sortedRowMap.map(_._1)
    println(rowNames.take(10))
    if (limit != -1) {
      rowNames = rowNames.take(limit)
    }

    val gis = probes.allGeneIds()
    val geneIds = rowNames.map(rn => gis.getOrElse(Probe(rn), Set.empty))
    geneIds.flatten.map(_.identifier).toArray
  }

  def sendFeedback(name: String, email: String, feedback: String): Unit = {
    val mm = getSessionData.matrixOption
    var state = "(No user state available)"
    if (mm != None && mm.get.current != null) {
      val cmat = mm.get.current
      state = "Matrix: " + cmat.rowKeys.size + " x " + cmat.columnKeys.size
      state += "\nColumns: " + cmat.columnKeys.mkString(", ")
    }
    Feedback.send(name, email, feedback, state, config.feedbackReceivers,
      config.feedbackFromAddress, context.config.appName)
  }
  
  @throws(classOf[NoDataLoadedException])
  def prepareHeatmap(groups: JList[Group], chosenProbes: JList[String],
    algorithm: Algorithm): String = {
    val data = new RandomData(userDir, 
      Array() ++ groups.map(_.getShortTitle), Array() ++ chosenProbes)
    
      val clust = new RClustering(data.userDir)
    
    clust.clustering(data.data.flatten, Array() ++ data.rowNames,
        Array() ++ data.colNames, 
        Array() ++ data.geneSymbols, algorithm)
    
//    prepareHeatmap(groups, chosenProbes, ValueType.Folds, algorithm)
  }

  @throws(classOf[NoDataLoadedException])
  def prepareHeatmap(groups: JList[Group], chosenProbes: JList[String],
    valueType: ValueType, algorithm: Algorithm): String = {

    //Reload data in a temporary controller if groups do not correspond to
    //the ones in the current session
    val cont = if (getSessionData.needsReload(groups, valueType)) {
      MatrixController(context, () => getOrthologs(context),
          groups, chosenProbes, valueType, false)
    } else {
      getSessionData.controller
    }

   val data = new ClusteringData(cont, probes, chosenProbes, valueType)

    val clust = new RClustering(userDir)
    clust.clustering(data.data.flatten, rowNamesForHeatmap(data.rowNames),
        data.colNames, data.geneSymbols, algorithm)
  }

  protected def rowNamesForHeatmap(names: Array[String]): Array[String] =
    names

}
