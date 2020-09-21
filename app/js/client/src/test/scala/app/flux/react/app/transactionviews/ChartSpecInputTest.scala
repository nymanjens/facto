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
        "single empty line" - {
          testRoundTrip(ChartSpec(Seq(Line())))
        }
        "single non-empty line" - {
          testRoundTrip(ChartSpec(Seq(Line(query = "ABC", inverted = true, cumulative = true))))
        }
        "multiple lines" - {
          testRoundTrip(
            ChartSpec(
              Seq(
                Line(query = "ABC", inverted = true, cumulative = true),
                Line(query = "DEF", inverted = true, cumulative = true),
                Line(),
                Line(query = "ABC", inverted = true, cumulative = true),
                Line(),
                Line(),
              )
            )
          )
        }
      }
    }
  }
}
