package app.flux.react.app.transactionviews

import scala.collection.immutable.Seq
import app.common.accounting.ChartSpec
import app.common.accounting.ChartSpec.Line
import utest._

object ChartSpecInputTest extends TestSuite {

  override def tests = TestSuite {
    "ChartSpec" - {
      "stringify and parseStringified" - {
        def testRoundTrip(chartSpec: ChartSpec) = {
          ChartSpec.parseStringified(chartSpec.stringify) ==> chartSpec
        }
        "singleEmptyLine" - {
          testRoundTrip(ChartSpec.singleEmptyLine)
        }
        "single non-empty line" - {
          testRoundTrip(
            ChartSpec(Seq(Line(name = "DEF@GHI", query = "ABC", inverted = true, cumulative = true)))
          )
        }
        "multiple lines" - {
          testRoundTrip(
            ChartSpec(
              Seq(
                Line(name = "@@@", query = "ABC", inverted = true, cumulative = true),
                Line(name = "|||", query = "DEF", inverted = true, cumulative = true),
                Line(name = "X", query = "", inverted = false, cumulative = false),
                Line(name = "Y", query = "ABC", inverted = true, cumulative = true),
                Line(name = "", query = "", inverted = false, cumulative = false),
                Line(name = "", query = "", inverted = false, cumulative = false),
              )
            )
          )
        }
      }
    }
  }
}
