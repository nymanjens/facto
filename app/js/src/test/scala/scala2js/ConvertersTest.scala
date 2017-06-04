package scala2js

import java.time.Month.MARCH

import common.time.LocalDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.EntityType
import utest._
import common.testing.TestObjects._
import models.User

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Converters._

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {
    "entityTypeToConverter" - {
      entityTypeToConverter(EntityType.UserType) ==> UserConverter
      entityTypeToConverter(EntityType.TransactionType) ==> TransactionConverter
      entityTypeToConverter(EntityType.TransactionGroupType) ==> TransactionGroupConverter
      entityTypeToConverter(EntityType.BalanceCheckType) ==> BalanceCheckConverter
      entityTypeToConverter(EntityType.ExchangeRateMeasurementType) ==> ExchangeRateMeasurementConverter
    }

    "seqConverter" - {
      val seq = Seq(1, 2)
      val jsValue = Scala2Js.toJs(seq)
      assert(jsValue.isInstanceOf[js.Array[_]])
      Scala2Js.toScala[Seq[Int]](jsValue) ==> seq
    }

    "seqConverter: testToJsAndBack" - {
      testToJsAndBack[Seq[String]](Seq("a", "b"))
      testToJsAndBack[Seq[String]](Seq())
    }

    "LocalDateTimeConverter: testToJsAndBack" - {
      testToJsAndBack[LocalDateTime](LocalDateTime.of(2022, MARCH, 13, 12, 13))
    }

    "UserConverter: testToJsAndBack" - {
      testToJsAndBack[User](testUser)
    }

    "TransactionConverter: testToJsAndBack" - {
      testToJsAndBack[Transaction](testTransactionWithId)
    }

    "TransactionGroup: testToJsAndBack" - {
      testToJsAndBack[TransactionGroup](testTransactionGroupWithId)
    }

    "BalanceCheck: testToJsAndBack" - {
      testToJsAndBack[BalanceCheck](testBalanceCheckWithId)
    }

    "ExchangeRateMeasurement: testToJsAndBack" - {
      testToJsAndBack[ExchangeRateMeasurement](testExchangeRateMeasurementWithId)
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value
  }
}
