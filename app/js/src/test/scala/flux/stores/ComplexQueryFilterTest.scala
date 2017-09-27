package flux.stores

import flux.stores.ComplexQueryFilter.{Prefix, QueryPart}
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object ComplexQueryFilterTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new common.testing.TestModule

    implicit val fakeDatabase = testModule.fakeRemoteDatabaseProxy
    implicit val userManager = testModule.entityAccess.userManager
    implicit val testAccountingConfig = testModule.testAccountingConfig

    val complexQueryStore = new ComplexQueryFilter()

    "parsePrefixAndSuffix()" - {
      "single colon" - {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } {
          complexQueryStore.parsePrefixAndSuffix(s"$prefixString:some value") ==>
            Some((prefix, "some value"))
        }
      }
      "multiple colons" - {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } {
          complexQueryStore
            .parsePrefixAndSuffix(s"$prefixString:some: value") ==> Some((prefix, "some: value"))
        }
      }
      "wrong prefix" - {
        complexQueryStore.parsePrefixAndSuffix(s"unknownPrefix:some: value") ==> None
      }
      "empty string" - {
        complexQueryStore.parsePrefixAndSuffix(s"") ==> None
      }
      "empty prefix" - {
        complexQueryStore.parsePrefixAndSuffix(s":some value") ==> None
      }
      "empty suffix" - {
        complexQueryStore.parsePrefixAndSuffix(s"category:") ==> None
      }
      "no colons" - {
        complexQueryStore.parsePrefixAndSuffix(s"category") ==> None
      }
    }

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
