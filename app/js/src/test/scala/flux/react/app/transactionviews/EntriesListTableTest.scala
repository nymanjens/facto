package flux.react.app.transactionviews

import java.lang

import common.testing.TestObjects._
import common.testing.{ReactTestWrapper, TestModule}
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.stores.AllEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import models.accounting._
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

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
