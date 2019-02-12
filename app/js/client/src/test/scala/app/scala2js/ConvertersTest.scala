package app.scala2js

import java.time.Month.MARCH

import app.common.testing.TestObjects._
import app.models.accounting._
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import app.scala2js.AppConverters._
import hydro.common.time.LocalDateTime
import hydro.scala2js.Scala2Js
import utest._

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {

    "fromEntityType" - {
      fromEntityType(User.Type) ==> UserConverter
      fromEntityType(Transaction.Type) ==> TransactionConverter
      fromEntityType(TransactionGroup.Type) ==> TransactionGroupConverter
      fromEntityType(BalanceCheck.Type) ==> BalanceCheckConverter
      fromEntityType(ExchangeRateMeasurement.Type) ==> ExchangeRateMeasurementConverter
    }

    "UserConverter" - {
      testToJsAndBack[User](testUserRedacted)
    }

    "TransactionConverter" - {
      testToJsAndBack[Transaction](testTransactionWithId)
    }

    "TransactionGroup" - {
      testToJsAndBack[TransactionGroup](testTransactionGroupWithId)
    }

    "BalanceCheck" - {
      testToJsAndBack[BalanceCheck](testBalanceCheckWithId)
    }

    "ExchangeRateMeasurement" - {
      testToJsAndBack[ExchangeRateMeasurement](testExchangeRateMeasurementWithId)
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value
  }
}
