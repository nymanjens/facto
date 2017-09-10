package jsfacades

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.ConvertersTest._
import scala2js.Keys

object LokiResultSetFakeTest extends TestSuite {

  override def tests = TestSuite {
    "LokiJs.ResultSet.Fake" - {
      val transaction1 = createTransaction(id = 1, categoryCode = "catA", day = 19)
      val transaction2 = createTransaction(id = 22, categoryCode = "catB", day = 18)
      val transaction3 = createTransaction(id = 33, categoryCode = "catB", day = 7)
      val resultSet = new LokiJs.ResultSet.Fake(Seq(transaction3, transaction1, transaction2))

      "filter()" - {
        resultSet.filter(Keys.Transaction.categoryCode, "catB").data().toSet ==> Set(
          transaction2,
          transaction3)
      }
      "filterGreaterThan() with greaterThan" - {
        resultSet.filterGreaterThan(Keys.id, 1L).data().toSet ==> Set(transaction2, transaction3)
        resultSet.filterGreaterThan(Keys.Transaction.createdDate, transaction3.createdDate).data().toSet ==>
          Set(transaction1, transaction2)
      }
      "filterLessThan()" - {
        resultSet.filterLessThan(Keys.id, 22L).data().toSet ==> Set(transaction1)
        resultSet.filterLessThan(Keys.Transaction.createdDate, transaction2.createdDate).data().toSet ==>
          Set(transaction3)
      }
      "sort()" - {
        resultSet.sort(LokiJs.Sorting.ascBy(Keys.id)).data() ==> Seq(
          transaction1,
          transaction2,
          transaction3)
        resultSet.sort(LokiJs.Sorting.descBy(Keys.id)).data() ==> Seq(
          transaction3,
          transaction2,
          transaction1)
        resultSet.sort(LokiJs.Sorting.ascBy(Keys.Transaction.categoryCode).thenDescBy(Keys.id)).data() ==>
          Seq(transaction1, transaction3, transaction2)
        resultSet.sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate)).data() ==>
          Seq(transaction3, transaction2, transaction1)
      }
      "limit()" - {
        resultSet.sort(LokiJs.Sorting.ascBy(Keys.id)).limit(2).data() ==> Seq(transaction1, transaction2)
      }
      "findOne()" - {
        resultSet.findOne(Keys.id, 22L) ==> Some(transaction2)
        resultSet.findOne(Keys.id, 44L) ==> None
      }
      "count()" - {
        resultSet.count() ==> 3
      }
    }
  }

  private def createTransaction(id: Int, categoryCode: String, day: Int): Transaction =
    testTransactionWithId
      .copy(
        idOption = Some(id),
        categoryCode = categoryCode,
        createdDate = createDateTime(2012, JANUARY, day))
}
