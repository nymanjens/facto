package app.scala2js

import java.time.Month.MARCH

import app.common.testing.TestObjects._
import app.models.access.ModelFields
import app.models.accounting._
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import app.scala2js.AppConverters._
import hydro.common.time.LocalDateTime
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.scala2js.Scala2Js
import hydro.scala2js.StandardConverters
import hydro.scala2js.StandardConverters._
import utest._

import scala.collection.immutable.Seq
import scala.scalajs.js

object ConvertersTest extends TestSuite {
  val dateTime = LocalDateTime.of(2022, MARCH, 13, 12, 13)

  override def tests = TestSuite {
    "fromModelField" - {
      StandardConverters
        .fromModelField(ModelFields.Transaction.categoryCode) ==> StandardConverters.StringConverter
      StandardConverters.fromModelField(ModelFields.id[User]) ==> StandardConverters.LongConverter
    }
    "LongConverter" - {
      "to JS and back" - {
        testToJsAndBack[Long](1L)
        testToJsAndBack[Long](0L)
        testToJsAndBack[Long](-1L)
        testToJsAndBack[Long](-12392913292L)
        testToJsAndBack[Long](911427549585351L) // 15 digits, which is the maximal javascript precision
        testToJsAndBack[Long](6886911427549585129L)
        testToJsAndBack[Long](-6886911427549585129L)
      }
      "Produces ordered results" - {
        val lower = Scala2Js.toJs(999L).asInstanceOf[String]
        val higher = Scala2Js.toJs(1000L).asInstanceOf[String]
        (lower < higher) ==> true
      }
    }

    "fromEntityType" - {
      fromEntityType(User.Type) ==> UserConverter
      fromEntityType(Transaction.Type) ==> TransactionConverter
      fromEntityType(TransactionGroup.Type) ==> TransactionGroupConverter
      fromEntityType(BalanceCheck.Type) ==> BalanceCheckConverter
      fromEntityType(ExchangeRateMeasurement.Type) ==> ExchangeRateMeasurementConverter
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

    "EntityTypeConverter" - {
      testToJsAndBack[EntityType.any](BalanceCheck.Type)
      testToJsAndBack[EntityType.any](Transaction.Type)
      testToJsAndBack[EntityType.any](User.Type)
    }

    "EntityModificationConverter" - {
      "Add" - {
        testToJsAndBack[EntityModification](EntityModification.Add(testTransactionWithIdA))
      }
      "Update" - {
        testToJsAndBack[EntityModification](EntityModification.Update(testUserA))
      }
      "Remove" - {
        testToJsAndBack[EntityModification](EntityModification.Remove[Transaction](19238))
      }
    }

    "UserConverter: testToJsAndBack" - {
      testToJsAndBack[User](testUserRedacted)
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