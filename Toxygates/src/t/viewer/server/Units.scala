package t.viewer.server

import t.common.shared.DataSchema
import t.common.shared.Pair
import t.common.shared.sample.Sample
import t.common.shared.sample.SampleClassUtils
import t.common.shared.sample.Unit
import t.db.SimpleVarianceSet
import t.db.VarianceSet
import t.model.SampleClass
import t.model.sample.CoreParameter._

import t.sparql.SampleClassFilter
import t.sparql.SampleFilter
import t.sparql.Samples
import t.viewer.server.Conversions._
import t.viewer.shared.TimeoutException


class Units(schema: DataSchema, sampleStore: Samples) extends
  UnitsHelper(schema) {
  /**
   * Generates units containing treated samples and their associated control samples.
   * TODO: sensitive algorithm, should simplify and possibly move to OTGTool.
   */
  @throws[TimeoutException]
  def units(sc: SampleClass,
      param: String, paramValues: Array[String])(implicit sf: SampleFilter) : Array[Pair[Unit, Unit]] = {

    //This will filter by the chosen parameter - usually compound name
    val rs = sampleStore.samples(SampleClassFilter(sc), param, paramValues.toSeq)
    units(sc, rs).map(x => new Pair(x._1, x._2.getOrElse(null))).toArray
  }

  def units(sc: SampleClass, samples: Iterable[t.db.Sample])
  (implicit sf: SampleFilter): Iterable[(Unit, Option[Unit])] = {
    def isControl(s: t.db.Sample) = schema.isSelectionControl(s.sampleClass)

    def unit(s: Sample) = SampleClassUtils.asUnit(s.sampleClass, schema)

    //TODO the copying may be costly - consider optimising in the future
    def unitWithoutMajorMedium(s: Sample) = unit(s).
      copyWithout(schema.majorParameter).copyWithout(schema.mediumParameter)

    def asUnit(ss: Iterable[Sample]) = new Unit(unit(ss.head), ss.toArray)

    val ss = samples.groupBy(x =>(
            x.sampleClass(Batch),
            x.sampleClass(ControlGroup)))

    val cgs = ss.keys.toSeq.map(_._2).distinct
    val potentialControls = sampleStore.samples(SampleClassFilter(sc), ControlGroup.id, cgs).
      filter(isControl).map(asJavaSample)

      /*
       * For each unit of treated samples inside a control group, all
       * control samples in that group are assigned as control,
       * assuming that other parameters are also compatible.
       */

    var r = Vector[(Unit, Option[Unit])]()
    for (((batch, cg), samples) <- ss;
        treated = samples.filter(!isControl(_)).map(asJavaSample)) {

      /*
       * Remove major parameter (compound in OTG case) as we now allow treated-control samples
       * to have different compound names.
       */

      val byUnit = treated.groupBy(unit(_))

      val treatedControl = byUnit.toSeq.map(tt => {
        val repSample = tt._2.head
        val repUnit = unitWithoutMajorMedium(repSample)

        val fcs = potentialControls.filter(s =>
          unitWithoutMajorMedium(s) == repUnit
          && s.get(ControlGroup) == repSample.get(ControlGroup)
          && s.get(Batch) == repSample.get(Batch)
          )

        val cu = if (fcs.isEmpty)
          new Unit(SampleClassUtils.asUnit(sc, schema), Array())
        else
          asUnit(fcs)

        val tu = asUnit(tt._2)

        (tu, Some(cu))
      })

      r ++= treatedControl

      for (
        (treat, Some(control)) <- treatedControl;
        samples = control.getSamples;
        if (!samples.isEmpty);
        pseudoUnit = (control, None)
      ) {
        r :+= pseudoUnit
      }
    }
    r
  }
}

class UnitsHelper(schema: DataSchema) {

  import t.model.sample.CoreParameter.{ControlGroup => ControlGroupParam}
  type ControlGroupKey = (String, String, String)

  //TODO try to simplify, share code with the above
  def byControlGroup(raw: Iterable[Unit]): Map[ControlGroupKey, Iterable[Unit]] =
    raw.groupBy(controlGroupKey)

  private val minorParameter = schema.minorParameter()

  def controlGroupKey(u: Unit): ControlGroupKey =
      controlGroupKey(u.getSamples()(0))

  def samplesByControlGroup(raw: Iterable[Sample]): Map[ControlGroupKey, Iterable[Sample]] =
    raw.groupBy(controlGroupKey)

  def controlGroupKey(s: Sample): ControlGroupKey =
      (s.get(ControlGroupParam), s.get(minorParameter), s.get(Batch))

  def unitGroupKey(s: Sample) = s.get(schema.mediumParameter())

  /**
   * Groups the samples provided into treated and control units, returning
   * a list of tuples whose first element is a treated unit and whose second
   * element is a tuple containing the corresponding control unit and variance
   * set.
   * @param samples samples to partition
   */
  def formControlUnitsAndVarianceSets(samples: Iterable[Sample]):
      Seq[(Unit, (Unit, VarianceSet))] = {
    formTreatedAndControlUnits(samples).flatMap {
      case (treatedSamples, controlSamples) =>
        val units = treatedSamples.map(formUnit(_, schema))
        val controlGroup = formUnit(controlSamples, schema)
        val varianceSet = new SimpleVarianceSet(controlSamples)
        units.map(_ -> (controlGroup, varianceSet))
    }
  }

  /**
   * Groups the samples provided into treated and control groups, returning
   * a list of tuples whose first element is the samples in a treated unit and
   * whose second element is the samples from the corresponding control unit.
   * @param samples samples to partition
   */
  def formTreatedAndControlUnits(samples: Iterable[Sample]):
      Seq[(Iterable[Iterable[Sample]], Iterable[Sample])] = {
    val controlGroups = samples.groupBy(controlGroupKey)
    controlGroups.map { case (unitKey, samples) =>
      samples.partition(!schema.isControl(_)) match {
        case (treated, controls) =>
          (treated.groupBy(unitGroupKey).values, controls)
      }
    }.toList
  }

  /**
   * Forms a unit from a set of samples.
   * @param samples all of the samples from the desired unit
   */
  def formUnit(samples: Iterable[Sample], schema:DataSchema): Unit = {
    new Unit(SampleClassUtils.asUnit(samples.head.sampleClass(), schema),
        samples.toArray)
  }
}
