/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t.db.file

import java.io._

import org.apache.commons.math3.stat.inference.TTest

import friedrich.util.CmdLineOptions
import t.db._
import scala.collection.mutable.HashMap
import scala.collection.{Map => CMap}

/**
 * log-2 fold values constructed from the input data
 * The sample space of the output may be smaller (control samples have no folds)
 */
abstract class FoldValueBuilder(md: Metadata, input: RawExpressionData)
  extends RawExpressionData {

  type Entry = (String, FoldPExpr)

  override def probes = input.probes
  
  def samples = input.samples.filter(!md.isControl(_))

  protected lazy val groups = md.treatedControlGroups(input.samples)

  def values(s: Sample): Seq[Entry] = {
    println("Compute control values")
    var r = List[Entry]()
    for ((ts, cs) <- groups;
      if ts.toSet.contains(s)) {
      println("Control barcodes: " + cs)
      println("Treated: " + ts)
      r = makeFolds(cs.toSeq, ts.toSeq, s, r)
    }
    r
  }

  /**
   * Construct fold values for a sample group.
   */
  protected def makeFolds(controlSamples: Seq[Sample],
      treatedSamples: Seq[Sample], sample: Sample,
      accumulator: List[Entry]): List[Entry]

  /**
   * Compute a control sample (as a mean).
   */
  protected def controlMeanSample(data: Seq[CMap[String, FoldPExpr]]): CMap[String, Double] = {
    var controlValues = HashMap[String, Double]()

    for (probe <- data.flatMap(_.keys).distinct) {
      val usableVals = data.flatMap(_.get(probe).map(_._1))
      if (usableVals.size > 0) {
        val mean = usableVals.sum / usableVals.size
        controlValues += (probe -> mean)
      }
    }
    controlValues
  }

  protected def foldPACall(log2fold: Double, controlCalls: Iterable[Char],
    treatedCalls: Iterable[Char]): Char = {

    //Treat M as A
    val controlPresent = controlCalls.count(_ == 'P') > controlCalls.size / 2
    val treatedPresent = treatedCalls.count(_ == 'P') > treatedCalls.size / 2
    if (log2fold > 0 && treatedPresent && !controlPresent) {
      'P'
    } else if (log2fold < 0 && !treatedPresent && controlPresent) {
      'P'
    } else if (treatedPresent && controlPresent) {
      'P'
    } else {
      'A'
    }
  }
}

/**
 * log-2 fold values with P-values.
 * This could be stored in a separate table, but for simplicity, we are grouping it with
 * expression data for now.
 */
class PFoldValueBuilder(md: Metadata, input: RawExpressionData)
  extends FoldValueBuilder(md, input) {
  val tt = new TTest

  /**
   * @param sample one of the treated samples to build fold values for.
   */
  override protected def makeFolds(controlSamples: Seq[Sample],
    treatedSamples: Seq[Sample],
    sample: Sample,
    accumulator: List[Entry]): List[Entry] = {

    val l2 = Math.log(2)
 
    val controlData = input.data(controlSamples)
    val treatedData = input.data(treatedSamples)
    val sampleExpr = treatedData(sample).mapValues(_._1)
    
    val controlValues = controlData.values.toSeq
    val treatedValues = treatedData.values.toSeq
    val controlMean = controlMeanSample(controlValues)
    
    val probes = input.probes.toSeq

    var r = accumulator
    for (p <- probes) {
      (sampleExpr.get(p), controlMean.get(p)) match {
        case (Some(v), Some(control)) =>
          
          val cs = controlValues.flatMap(_.get(p)).map(_._1)
          val ts = treatedValues.flatMap(_.get(p)).map(_._1)
          val pval = if (cs.size >= 2 && ts.size >= 2) {
            tt.tTest(cs.toArray, ts.toArray)
          } else {
            Double.NaN
          }
          
          val foldVal = Math.log(v / control) / l2
          val controlCalls = controlValues.flatMap(_.get(p).map(_._2))
          val treatedCalls = treatedValues.flatMap(_.get(p).map(_._2))
          val pacall = foldPACall(foldVal, controlCalls, treatedCalls)
          r ::= (p, (foldVal, pacall, pval))
        case _ =>
          r ::= (p, (Double.NaN, 'A', Double.NaN))
      }
    }
    r
  }

  import scala.collection.{Map => CMap}

  def data(s: Sample): CMap[String, FoldPExpr] = {       
    HashMap() ++ values(s)
  }
}

