package otgviewer.server.rpc

import scala.collection.JavaConversions._

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import t.common.shared.SampleClass
import t.viewer.server.Configuration
import t.common.shared.AType

object SparqlServiceTest {  
  val testClass = Map("sin_rep_type" -> "Single",
      "organism" -> "Rat",
      "organ_id" -> "Liver",
      "test_type" -> "in vivo")
      
  def testSampleClass = new SampleClass(mapAsJavaMap(testClass))
}

@RunWith(classOf[JUnitRunner])
class SparqlServiceTest extends FunSuite with BeforeAndAfter {

  var s: SparqlServiceImpl = _
  
  before {    
    val conf = new Configuration("otg", "/shiba/toxygates", 2)    
    s = new SparqlServiceImpl()
    s.localInit(conf)
  }  
  
  after {
    s.destroy
  }
  
  val sc = SparqlServiceTest.testSampleClass
  
  val probes = Array("1387936_at", "1391544_at")
//  val geneIds = Array("361510", "362972")
  
  private def testAssociation(typ: AType) = s.associations(sc, Array(typ), probes)
    
  test("BP GO terms") {
    testAssociation(AType.GOBP)
  }
  
  test("CC GO terms") {
    testAssociation(AType.GOCC)
  }
  
  test("MF GO terms") {
    testAssociation(AType.GOMF)
  }
  
  test ("KEGG pathways") {
    testAssociation(AType.KEGG)
  }
  
  test ("UniProt") {
    testAssociation(AType.Uniprot)
  }
  
  test ("OrthProts") {
    testAssociation(AType.OrthProts)
  }
  
  test ("CHEMBL") {
    testAssociation(AType.Chembl)
  }
  
  test ("DrugBank") {
    testAssociation(AType.Drugbank)
  }
  
  test ("Genes for pathway") {
    val ps = s.probesForPathway(sc, "Glutathione metabolism")
    println(ps.size + " probes")
    assert(ps.size === 42)
  }
}