package jsfacades

import java.time.Month.{JANUARY, MARCH}

import common.time.LocalDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.EntityType
import utest._

import scala2js.Converters._
import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import models.User

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Converters._
import scala2js.ConvertersTest._
import scala2js.Scala2Js

object LokiResultSetFakeTest extends TestSuite {

  override def tests = TestSuite {
    "Loki.ResultSet.Fake" - {
      val transaction1 = createTransaction(id = 1, categoryCode = "catA", day = 19)
      val transaction2 = createTransaction(id = 22, categoryCode = "catB", day = 18)
      val transaction3 = createTransaction(id = 33, categoryCode = "catB", day = 7)
      val resultSet = new Loki.ResultSet.Fake(Seq(transaction3, transaction1, transaction2))

      "find()" - {
        resultSet.find("categoryCode" -> "catB").data().toSet ==> Set(transaction2, transaction3)
      }
      "find() with lessThan" - {
        resultSet.find("id" -> Loki.ResultSet.lessThan(22)).data().toSet ==> Set(transaction1)
        resultSet.find("createdDate" -> Loki.ResultSet.lessThan(transaction2.createdDate)).data().toSet ==>
          Set(transaction3)
      }
      "find() with greaterThan" - {
        resultSet.find("id" -> Loki.ResultSet.greaterThan(1)).data().toSet ==>
          Set(transaction2, transaction3)
        resultSet
          .find("createdDate" -> Loki.ResultSet.greaterThan(transaction3.createdDate))
          .data()
          .toSet ==>
          Set(transaction1, transaction2)
      }
      "sort()" - {
        resultSet.sort(Loki.Sorting.ascBy("id")).data() ==> Seq(transaction1, transaction2, transaction3)
        resultSet.sort(Loki.Sorting.descBy("id")).data() ==> Seq(transaction3, transaction2, transaction1)
        resultSet.sort(Loki.Sorting.ascBy("categoryCode").thenDescBy("id")).data() ==>
          Seq(transaction1, transaction3, transaction2)
        resultSet.sort(Loki.Sorting.ascBy("createdDate")).data() ==>
          Seq(transaction3, transaction2, transaction1)
      }
      "limit()" - {
        resultSet.sort(Loki.Sorting.ascBy("id")).limit(2).data() ==> Seq(transaction1, transaction2)
      }
      "findOne()" - {
        resultSet.findOne("id" -> "22") ==> Some(transaction2)
        resultSet.findOne("id" -> "44") ==> None
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
