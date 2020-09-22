package app.common.accounting

import app.common.accounting.ChartSpec.Line
import utest.TestSuite
import utest._

import scala.collection.immutable.Seq

object ChartSpecTest extends TestSuite {

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
                Line(name = "@@@", query = "ABC", inverted = true, cumulative = true, showBars = true),
                Line(name = "|||", query = "DEF", inverted = true, cumulative = true),
                Line(name = "X", query = ""),
                Line(name = "Y", query = "ABC", inverted = true, cumulative = true),
                Line(name = "", query = ""),
                Line(name = "", query = ""),
              )
            )
          )
        }
      }
    }
  }
}
