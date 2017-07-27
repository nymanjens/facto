package jsfacades

import java.time.Month.MARCH

import common.time.LocalDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.EntityType
import utest._
import scala2js.Converters._
import common.testing.TestObjects._
import models.User

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Converters._
import scala2js.ConvertersTest._
import scala2js.Scala2Js

object LokiResultSetFakeTest extends TestSuite {

  override def tests = TestSuite {
    "Loki.ResultSet.Fake" - {
      val transaction1 = testTransactionWithId.copy(idOption = Some(1), categoryCode = "catA")
      val transaction2 = testTransactionWithId.copy(idOption = Some(2), categoryCode = "catB")
      val transaction3 = testTransactionWithId.copy(idOption = Some(3), categoryCode = "catB")
      val resultSet = new Loki.ResultSet.Fake(Seq(transaction3, transaction1, transaction2))

      "find()" - {
        resultSet.find("categoryCode" -> "catB").data().toSet ==> Set(transaction2, transaction3)
      }
      "sort()" - {
        resultSet.sort(Loki.Sorting.ascBy("id")).data() ==> Seq(transaction1, transaction2, transaction3)
        resultSet.sort(Loki.Sorting.descBy("id")).data() ==> Seq(transaction3, transaction2, transaction1)
        resultSet.sort(Loki.Sorting.ascBy("categoryCode").thenDescBy("id")).data() ==>
          Seq(transaction1, transaction3, transaction2)
      }
      "limit()" - {
        resultSet.sort(Loki.Sorting.ascBy("id")).limit(2).data() ==> Seq(transaction1, transaction2)
      }
      "findOne()" - {
        resultSet.findOne("id" -> "2") ==> Some(transaction2)
        resultSet.findOne("id" -> "4") ==> None
      }
      "count()" - {
        resultSet.count() ==> 3
      }
    }
  }
}
