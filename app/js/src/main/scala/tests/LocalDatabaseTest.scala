package models.access

import common.testing.TestObjects._
import models.User
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.accounting.money.ExchangeRateMeasurement
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.manager.EntityModification
import tests.ManualTests.{ManualTest, ManualTestSuite}

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
object LocalDatabaseTest extends ManualTestSuite {

  override def tests = Seq(
    ManualTest("isEmpty") {
      LocalDatabase.createInMemoryForTests() flatMap { db =>
        db.isEmpty() ==> true
        db.addAll(Seq(testTransactionWithId))
        db.isEmpty() ==> false

        db.clear() map { _ =>
          db.isEmpty() ==> true
          db.setSingletonValue(NextUpdateTokenKey, testDate)
          db.isEmpty() ==> false
        }
      }
    },

    ManualTest("setSingletonValue") {
      LocalDatabase.createInMemoryForTests() map { db =>
        db.getSingletonValue(VersionKey).isDefined ==> false

        db.setSingletonValue(VersionKey, "abc")
        db.getSingletonValue(VersionKey).get ==> "abc"

        db.setSingletonValue(NextUpdateTokenKey, testDate)
        db.getSingletonValue(NextUpdateTokenKey).get ==> testDate
      }
    },

    ManualTest("save") {
      LocalDatabase.createStoredForTests() flatMap { db =>
        db.clear() flatMap { _ =>
          db.addAll(Seq(testTransactionWithId))
          db.setSingletonValue(VersionKey, "testVersion")

          db.save() flatMap { _ =>
            db.setSingletonValue(VersionKey, "otherTestVersion")
            LocalDatabase.createStoredForTests() map { otherDb =>
              otherDb.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
              otherDb.getSingletonValue(VersionKey).get ==> "testVersion"
            }
          }
        }
      }
    },

    ManualTest("newQuery(): Lookup by ID works") {
      LocalDatabase.createInMemoryForTests() map { db =>
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))
        db.addAll(Seq(testTransactionWithId, transaction2, transaction3))

        db.newQuery[Transaction]().findOne("id" -> "99992") ==> Some(transaction2)
      }
    },

    ManualTest("clear") {
      LocalDatabase.createInMemoryForTests() flatMap { db =>
        db.addAll(Seq(testTransactionWithId))
        db.setSingletonValue(VersionKey, "testVersion")

        db.clear() map { _ =>
          db.isEmpty() ==> true
        }
      }
    },

    ManualTest("addAll") {
      LocalDatabase.createInMemoryForTests() map { db =>
        db.addAll(Seq(testUser))
        db.addAll(Seq(testTransactionWithId))
        db.addAll(Seq(testTransactionGroupWithId))
        db.addAll(Seq(testBalanceCheckWithId))
        db.addAll(Seq(testExchangeRateMeasurementWithId))

        db.newQuery[User]().data() ==> Seq(testUser)
        db.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
        db.newQuery[TransactionGroup]().data() ==> Seq(testTransactionGroupWithId)
        db.newQuery[BalanceCheck]().data() ==> Seq(testBalanceCheckWithId)
        db.newQuery[ExchangeRateMeasurement]().data() ==> Seq(testExchangeRateMeasurementWithId)
      }
    },

    ManualTest("addAll: Inserts no duplicates IDs") {
      LocalDatabase.createInMemoryForTests() map { db =>
        val transactionWithSameIdA = testTransactionWithId.copy(categoryCode = "codeA")
        val transactionWithSameIdB = testTransactionWithId.copy(categoryCode = "codeB")
        db.addAll(Seq(testTransactionWithId, transactionWithSameIdA))
        db.addAll(Seq(testTransactionWithId, transactionWithSameIdB))

        db.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
      }
    },

    ManualTest("applyModifications") {
      LocalDatabase.createInMemoryForTests() map { db =>
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        db.addAll(Seq(testTransactionWithId))

        db.applyModifications(Seq(
          EntityModification.Add(transaction2),
          EntityModification.createDelete(testTransactionWithId)
        ))

        db.newQuery[Transaction]().data() ==> Seq(transaction2)
      }
    },

    ManualTest("applyModifications: Is idempotent") {
      LocalDatabase.createInMemoryForTests() map { db =>
        val transactionWithSameId = testTransactionWithId.copy(categoryCode = "codeA")
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))

        db.applyModifications(Seq(
          EntityModification.Add(testTransactionWithId),
          EntityModification.Add(testTransactionWithId),
          EntityModification.Add(transactionWithSameId),
          EntityModification.Add(transaction2),
          EntityModification.createDelete(transaction2),
          EntityModification.createDelete(transaction2),
          EntityModification.createDelete(transaction3)
        ))

        db.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
      }
    }
  )
}
