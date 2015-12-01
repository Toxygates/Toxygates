/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
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

package t

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import t.db.testing.FakeBasicMatrixDB
import t.testing.FakeContext
import t.db.SampleIndex
import t.db.Sample
import t.db.ProbeMap
import t.db.BasicExprValue
import t.db.BasicValueInsert
import t.db.testing.TestData

@RunWith(classOf[JUnitRunner])
class MatrixInsertTest extends TTestSuite {

  import TestData._

  test("Absolute value insertion") {
    val data = makeTestData(false)
    val db = new FakeBasicMatrixDB()

    val ins = new BasicValueInsert(db, data) {
      def mkValue(v: Double, c: Char, p: Double) =
       BasicExprValue(v, c)
    }

    ins.insert("Absolute value data insert").run()
    val s1 = db.records.map(ev => (ev._1, context.probeMap.unpack(ev._2),
        ev._3.value, ev._3.call)).toSet
    val s2 = data.data.flatMap(x => x._2.map(y => (x._1, y._1, y._2._1, y._2._2))).toSet
    s1 should equal(s2)
  }

  //  test("Folds") {
  //    val data = makeTestData()
  //    val db = new FakeMicroarrayDB()
  //    val builder = new BasicFoldValueBuilder()
  //  }
}