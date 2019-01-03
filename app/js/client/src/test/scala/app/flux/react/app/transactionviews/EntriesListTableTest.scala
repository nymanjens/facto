package app.flux.react.app.transactionviews

import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import utest._

import scala.collection.immutable.Seq

object EntriesListTableTest extends TestSuite {

  override def tests = TestSuite {
    "NumEntriesStrategy" - {
      "validates arguments" - {
        NumEntriesStrategy(3)
        NumEntriesStrategy(3, Seq(4))
        NumEntriesStrategy(3, Seq(4, 5))

        assertInvalid(NumEntriesStrategy(3, Seq(2)))
        assertInvalid(NumEntriesStrategy(3, Seq(5, 4)))
      }
    }
  }

  private def assertInvalid(code: => Any): Unit = {
    try {
      code
      throw new java.lang.AssertionError("Expected exception")
    } catch {
      case e: IllegalArgumentException => // expected
    }
  }
}
