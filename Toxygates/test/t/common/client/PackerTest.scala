package t.common.client

import org.scalatest._
import otg.model.sample.AttributeSet
import t.model.SampleClass
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import otg.model.sample.OTGAttribute
import t.viewer.client.Packer

/**
 * Tests for unpacking model classes from string input.
 */
@RunWith(classOf[JUnitRunner])
class PackerTest extends FunSuite with Matchers {
  val attributes = AttributeSet.getDefault

//  test("unpackSampleClass gracefully handles an odd number of input tokens") {
//    val input = "test_type,,,SAT,,,sin_rep_type"
//    var sampleClass = Packer.unpackSampleClass(attributes, input)
//
//    sampleClass shouldBe a [SampleClass]
//    // We should end up with two keys: test_type, which was in the input, and
//    // type, which is added by unpackSampleClass. sin_rep_type, which didn't have
//    // a value, should not be in there.
//    sampleClass.getKeys.size shouldBe 2
//    sampleClass.getKeys.contains(OTGAttribute.Repeat) shouldBe false
//    sampleClass.get(OTGAttribute.TestType) shouldBe "SAT"
//  }
//
//  test("unpackSampleClass ignores invalid attributes") {
//    val input = "asdf,,,42,,,test_type,,,SAT"
//    var sampleClass = Packer.unpackSampleClass(attributes, input)
//
//    sampleClass shouldBe a [SampleClass]
//    sampleClass.getKeys.size shouldBe 2 // as above, type is added by unpackSampleClass
//    sampleClass.get(OTGAttribute.TestType) shouldBe "SAT"
//  }
}
