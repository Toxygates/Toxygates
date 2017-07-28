package t.viewer.server

import t.sparql.SampleClassFilter
import t.model.SampleClass
import t.common.shared.sample.SampleClassUtils
import t.viewer.shared.TimeoutException
import t.common.shared.sample.Sample
import t.common.shared.Pair
import t.common.shared.DataSchema
import t.common.shared.sample.Unit
import t.sparql.Samples
import t.sparql.SampleFilter
import t.viewer.server.Conversions._

class Units(schema: DataSchema, sampleStore: Samples) {
    /**
   * Generates units containing treated samples and their associated control samples.
   * TODO: sensitive algorithm, should simplify and possibly move to OTGTool.
   */
  @throws[TimeoutException]
  def units(sc: SampleClass,
      param: String, paramValues: Array[String])(implicit sf: SampleFilter) : Array[Pair[Unit, Unit]] = {      
    
    def isControl(s: t.db.Sample) = schema.isSelectionControl(s.sampleClass)

    def unit(s: Sample) = SampleClassUtils.asUnit(s.sampleClass, schema)

    //TODO the copying may be costly - consider optimising in the future
    def unitWithoutMajorMedium(s: Sample) = unit(s).
      copyWithout(schema.majorParameter).copyWithout(schema.mediumParameter())

    def asUnit(ss: Iterable[Sample]) = new Unit(unit(ss.head), ss.toArray)

    //This will filter by the chosen parameter - usually compound name

    import t.db.SampleParameters._

    val rs = sampleStore.samples(SampleClassFilter(sc), param, paramValues.toSeq)
    val ss = rs.groupBy(x =>(
            x.sampleClass("batchGraph"),
            x.sampleClass(ControlGroup.id)))

    val cgs = ss.keys.toSeq.map(_._2).distinct
    val potentialControls = sampleStore.samples(SampleClassFilter(sc), ControlGroup.id, cgs).
      filter(isControl).map(asJavaSample)

      /*
       * For each unit of treated samples inside a control group, all
       * control samples in that group are assigned as control,
       * assuming that other parameters are also compatible.
       */

    var r = Vector[Pair[Unit, Unit]]()
    for (((batch, cg), samples) <- ss;
        treated = samples.filter(!isControl(_)).map(asJavaSample)) {

      /*
       * Remove major parameter (compound in OTG case) as we now allow treated-control samples
       * to have different compound names.
       */

      val byUnit = treated.groupBy(unit(_))

      val treatedControl = byUnit.map(tt => {
        val repSample = tt._2.head
        val repUnit = unitWithoutMajorMedium(repSample)

        val fcs = potentialControls.filter(s =>
          unitWithoutMajorMedium(s) == repUnit
          && s.get(ControlGroup.id) == repSample.get(ControlGroup.id)
          && s.get(BatchGraph.id) == repSample.get(BatchGraph.id)
          )

        val cu = if (fcs.isEmpty)
          new Unit(SampleClassUtils.asUnit(sc, schema), Array())
        else
          asUnit(fcs)

        val tu = asUnit(tt._2)

        new Pair(tu, cu)
      })

      r ++= treatedControl

      r ++= treatedControl.flatMap(tc =>
        if (!tc.second().getSamples.isEmpty)
          //add this as a pseudo-treated unit by itself
          Some(new Pair(tc.second(), null: Unit))
        else
          None
          )
    }
    r.toArray
  }
}