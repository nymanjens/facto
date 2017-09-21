package flux.stores

import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._
import flux.stores.ComplexQueryStore.QueryPart

object ComplexQueryStoreTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new common.testing.TestModule

    implicit val fakeDatabase = testModule.fakeRemoteDatabaseProxy

    val complexQueryStore = new ComplexQueryStore()

    "splitInParts()" - {
      "empty string" - {
        complexQueryStore.splitInParts("") ==> Seq()
      }
      "negation" - {
        complexQueryStore.splitInParts("-a c -def") ==>
          Seq(QueryPart.not("a"), QueryPart("c"), QueryPart.not("def"))
      }
      "double negation" - {
        complexQueryStore.splitInParts("--a") ==> Seq(QueryPart.not("-a"))
      }
      "double quotes" - {
        complexQueryStore.splitInParts(""" "-a c" """) ==> Seq(QueryPart("-a c"))
      }
      "single quotes" - {
        complexQueryStore.splitInParts(" '-a c' ") ==> Seq(QueryPart("-a c"))
      }
      "negated quotes" - {
        complexQueryStore.splitInParts("-'XX YY'") ==> Seq(QueryPart.not("XX YY"))
      }
      "quote inside text" - {
        complexQueryStore.splitInParts("-don't won't") ==> Seq(QueryPart.not("don't"), QueryPart("won't"))
      }
    }
  }
}
