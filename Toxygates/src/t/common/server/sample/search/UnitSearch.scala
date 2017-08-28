package t.common.server.sample.search

import t.viewer.server.Conversions._
import t.viewer.server.Annotations
import t.db.VarianceSet
import org.stringtemplate.v4.ST
import t.common.shared.DataSchema
import t.common.shared.sample.Unit
import t.common.shared.sample.search.MatchCondition
import t.db.Metadata
import t.model.sample.Attribute
import otg.model.sample.OTGAttribute._

object UnitSearch extends SearchCompanion[Unit, UnitSearch] {

  protected def create(metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Unit, VarianceSet],
    samples: Iterable[Unit],
    searchParams: Iterable[Attribute]) =
      new UnitSearch(metadata, condition, controlGroups, samples, searchParams)

  /**
   * Preprocess a Unit to prepare it for searching. For each search parameter,
   * computes the average value for samples in the unit, and stores it as the
   * parameter value for the unit.
   */
  override protected def preprocessSample(metadata: Metadata, searchParams: Iterable[Attribute]) =
    (unit: Unit) => {
      val samples = unit.getSamples
      for (param <- searchParams) {
        val paramId = param.id

        unit.put(paramId, try {
          var sum: Option[Double] = None
          var count: Int = 0;

          for (sample <- samples) {
            val scalaSample = asScalaSample(sample)

            sum = metadata.parameter(scalaSample, paramId) match {
              case Some("NA") => sum
              case Some(str)  => {
                count = count + 1
                sum match {
                  case Some(x) => Some(x + str.toDouble)
                  case None    => Some(str.toDouble)
                }
              }
              case None       => sum
            }
          }

          sum match {
            case Some(x) => {
              (x / count).toString()
            }
            case None    => null
          }
        } catch {
          case nf: NumberFormatException => null
        })
      }
      unit
    }

  protected def formControlGroups(metadata: Metadata, annotations: Annotations) = (units: Iterable[Unit]) => {
    val sampleControlGroups = annotations.controlGroups(units.flatMap(_.getSamples()), metadata)
    Map() ++
      units.map(unit => {
        unit.getSamples.headOption match {
          case Some(s) =>
            sampleControlGroups.get(s) match {
              case Some(cg) => Some(unit -> cg)
              case _ => None
            }
          case _ => None
        }
      }).flatten
  }

  protected def isControlSample(schema: DataSchema) =
    schema.isControl(_)
}

class UnitSearch(metadata: Metadata, condition: MatchCondition,
    controlGroups: Map[Unit, VarianceSet], samples: Iterable[Unit], searchParams: Iterable[Attribute])
    extends AbstractSampleSearch[Unit](metadata, condition,
        controlGroups, samples, searchParams)  {

  protected def sampleAttributeValue(unit: Unit, param: Attribute): Option[Double] =
    try {
      Option(unit.get(param)) match {
        case Some(v) => Some(v.toDouble)
        case None    => None
      }
    } catch {
      case nf: NumberFormatException => None
    }

  protected def postMatchAdjust(unit: Unit): Unit =
    unit

  protected def zTestSampleSize(unit: Unit): Int =
    unit.getSamples().length

  protected def sortObject(unit: Unit): (String, Int, Int) = {
    (unit.get(Compound), doseLevelMap.getOrElse((unit.get(DoseLevel)), Int.MaxValue),
        exposureTimeMap.getOrElse((unit.get(ExposureTime)), Int.MaxValue))
  }
}
