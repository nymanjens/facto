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

object LokiTest extends TestSuite {

  override def tests = TestSuite {
    "Loki.ResultSet.Fake" - {
      val transaction1 = testTransactionWithId.copy(idOption = Some(1), categoryCode = "catB")
      val transaction2 = testTransactionWithId.copy(idOption = Some(3), categoryCode = "catA")
      val transaction3 = testTransactionWithId.copy(idOption = Some(2), categoryCode = "catA")
      val resultSet = new Loki.ResultSet.Fake(Seq(transaction1, transaction2, transaction3))

      "find()" - {
        resultSet.find("categoryCode" -> "catA").data() ==> Seq(transaction2, transaction3)
      }
      "sort()" - {
        resultSet.sort("id", isDesc = false).data() ==> Seq(transaction1, transaction3, transaction2)
        resultSet.sort("id", isDesc = true).data() ==> Seq(transaction2, transaction3, transaction1)
      }
      "limit()" - {
        resultSet.limit(2).data() ==> Seq(transaction1, transaction2)
      }
      "findOne()" - {
        resultSet.findOne("id" -> "2") ==> Some(transaction3)
        resultSet.findOne("id" -> "4") ==> None
      }
      "count()" - {
        resultSet.count() ==> 3
      }
    }
  }
}
