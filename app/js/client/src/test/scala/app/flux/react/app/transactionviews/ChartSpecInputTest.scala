package app.flux.react.app.transactionviews

import scala.collection.immutable.Seq
import app.flux.react.app.transactionviews.ChartSpecInput.ChartSpec
import app.flux.react.app.transactionviews.ChartSpecInput.Line
import utest._

object ChartSpecInputTest extends TestSuite {

  override def tests = TestSuite {
    "ChartSpec" - {
      "stringify and parseStringified" - {
        def testRoundTrip(chartSpec: ChartSpec) = {
          ChartSpec.parseStringified(chartSpec.stringify) ==> chartSpec
        }
        "empty" - {
          testRoundTrip(ChartSpec.empty)
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
