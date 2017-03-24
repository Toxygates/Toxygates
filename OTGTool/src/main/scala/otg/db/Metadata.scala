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

package otg.db

import scala.collection.immutable.ListMap

import t.db.ParameterSet
import t.db.Sample

/**
 * Information about a set of samples.
 */
trait Metadata extends t.db.Metadata {

  override def isControl(s: Sample): Boolean = parameter(s, "dose_level") == "Control"

  /**
   * Retrieve the set of compounds that exist in this metadata set.
   */
  def compounds: Set[String] = parameterValues("compound_name")

  /**
   * Retrieve the compound associated with a particular sample.
   */
  def compound(s: Sample): String = parameter(s, "compound_name")

}

object OTGParameterSet extends ParameterSet {

  /**
   * Map human-readable descriptions to the keys we expect to find in the RDF data.
   * The RDF predicates are of the form <http://127.0.0.1:3333/(key)>.
   */

  private val nonNumericalKeys = ListMap(
    "Sample ID" -> "sample_id",
    "Platform ID" -> "platform_id",
    "Experiment ID" -> "exp_id",
    "Control group" -> "control_group",
    "Group ID" -> "group_id",
    "Individual ID" -> "individual_id",
    "Organ" -> "organ_id",
    "Material ID" -> "material_id",
    "Compound" -> "compound_name",
    "Compound abbreviation" -> "compound_abbr",
    "Compound no" -> "compound_no",
    "CAS number" -> "cas_number",
    "KEGG drug" -> "kegg_drug",
    "KEGG compound" -> "kegg_compound",
    "Organism" -> "organism",
    "Test type" -> "test_type",
    "Repeat?" -> "sin_rep_type",
    "Sex" -> "sex_type",
    "Strain" -> "strain_type",
    "Administration route" -> "adm_route_type",
    "Animal age (weeks)" -> "animal_age_week",
    "Exposure time" -> "exposure_time",
    "Dose" -> "dose",
    "Dose unit" -> "dose_unit",
    "Dose level" -> "dose_level",
    "Medium type" -> "medium_type",
    "Product information" -> "product_information",
    "CRO type" -> "cro_type")

  /**
   * These are the annotations that it makes sense to consider in a
   * statistical, numerical way.
   */
  private val numericalKeys = ListMap("Terminal body weight (g)" -> "terminal_bw",
    "Liver weight (g)" -> "liver_wt",
    "Kidney weight total (g)" -> "kidney_total_wt",
    "Kidney weight left (g)" -> "kidney_wt_left",
    "Kidney weight right (g)" -> "kidney_wt_right",
    "RBC (x 10^4/uL)" -> "RBC",
    "Hb (g/DL)" -> "Hb",
    "Ht (%)" -> "Ht",
    "MCV (fL)" -> "MCV",
    "MCH (pg)" -> "MCH",
    "MCHC (%)" -> "MCHC",
    "Ret (%)" -> "Ret",
    "Plat (x 10^4/uL)" -> "Plat",
    "WBC (x 10^2/uL)" -> "WBC",
    "Neutrophil (%)" -> "Neu",
    "Eosinophil (%)" -> "Eos",
    "Basophil (%)" -> "Bas",
    "Monocyte (%)" -> "Mono",
    "Lymphocyte (%)" -> "Lym",
    "PT (s)" -> "PT",
    "APTT (s)" -> "APTT",
    "Fbg (mg/dL)" -> "Fbg",
    "ALP (IU/L)" -> "ALP",
    "TC (mg/dL)" -> "TC",
    "TBIL (mg/dL)" -> "TBIL",
    "DBIL (mg/dL)" -> "DBIL",
    "GLC (mg/dL)" -> "GLC",
    "BUN (mg/dL)" -> "BUN",
    "CRE (mg/dL)" -> "CRE",
    "Na (meq/L)" -> "Na",
    "K (meq/L)" -> "K",
    "Cl (meq/L)" -> "Cl",
    "Ca (mg/dL)" -> "Ca",
    "IP (mg/dL)" -> "IP",
    "TP (g/dL)" -> "TP",
    "RALB (g/dL)" -> "RALB",
    "A / G ratio" -> "AGratio",
    "AST (GOT) (IU/L)" -> "AST",
    "ALT (GPT) (IU/L)" -> "ALT",
    "LDH (IU/L)" -> "LDH",
    "gamma-GTP (IU/L)" -> "GTP",
    "DNA (%)" -> "DNA")

  //TODO reconsider this method
  def postReadAdjustment(kv: (String, String)): String = kv._1 match {
    case "CAS number" => {
      val s = kv._2.split("3333/")
      if (s.size > 1) {
        s(1)
      } else {
        "N/A"
      }
    }
    //expect e.g. http://bio2rdf.org/dr:D00268
    case "KEGG drug" | "KEGG compound" => {
      val s = kv._1.split(":")
      if (s.size > 2) {
        s(2)
      } else {
        "N/A"
      }
    }
    case _ => kv._2
  }

  val all =
    (numericalKeys ++ nonNumericalKeys).map(x => t.db.SampleParameter(x._2, x._1))

  def isNumerical(p: t.db.SampleParameter) =
    numericalKeys.values.toSet.contains(p.identifier)

  val highLevel = List("organism", "test_type", "sin_rep_type", "organ_id").map(byId)

  val required = highLevel ++ List("sample_id",
    "compound_name", "dose_level", "exposure_time",
    "platform_id", "control_group").map(byId)

  override val previewDisplay = List("dose", "dose_unit", "dose_level",
    "exposure_time", "adm_route_type").map(byId)

  /**
   * Find the files that are control samples in the collection that a given barcode
   * belongs to.
   *
   * TODO think about ways to generalise this without depending on the
   * key/value of dose_level = Control. Possibly depend on DataSchema
   */
  override def controlSamples(metadata: t.db.Metadata, s: Sample): Iterable[Sample] = {
    val params = metadata.parameterMap(s)
    val expTime = params("exposure_time")
    val cgroup = params("control_group")

    println(expTime + " " + cgroup)
    metadata.samples.filter(s => {
      val params = metadata.parameterMap(s)
      params("exposure_time") == expTime &&
      params("control_group") == cgroup &&
      metadata.isControl(s)
    })
  }

  override def treatedControlGroups(metadata: t.db.Metadata, ss: Iterable[Sample]):
    Iterable[(Iterable[Sample], Iterable[Sample])] = {
    for (
      (cs, ts) <- ss.groupBy(controlSamples(metadata, _));
      (d, dts) <- ts.groupBy(metadata.parameter(_, "dose_level"))
    ) yield ((dts, cs))
  }
}
