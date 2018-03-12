package tests

import common.testing.TestObjects._
import models.access.LocalDatabase
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.modification.EntityModification
import models.money.ExchangeRateMeasurement
import models.user.User
import tests.ManualTests.{ManualTest, ManualTestSuite}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] object LocalDatabaseTest extends ManualTestSuite {

  private val encryptionSecret = "gA5t6NkQaFpOZsBEU45bZgwlwi7Zeb"

  override def tests = Seq(
    ManualTest("isEmpty") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        db.isEmpty ==> true
        db.addAll(Seq(testTransactionWithId))
        db.isEmpty ==> false

        await(db.clear())

        db.isEmpty ==> true
        db.setSingletonValue(NextUpdateTokenKey, testDate)
        db.isEmpty ==> false
      }
    },
    ManualTest("setSingletonValue") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        db.getSingletonValue(VersionKey).isDefined ==> false

        db.setSingletonValue(VersionKey, "abc")
        db.getSingletonValue(VersionKey).get ==> "abc"

        db.setSingletonValue(NextUpdateTokenKey, testDate)
        db.getSingletonValue(NextUpdateTokenKey).get ==> testDate
      }
    },
    ManualTest("save") {
      async {
        val db = await(LocalDatabase.createStoredForTests(encryptionSecret))
        await(db.clear())
        db.addAll(Seq(testTransactionWithId))
        db.setSingletonValue(VersionKey, "testVersion")

        await(db.save())
        db.setSingletonValue(VersionKey, "otherTestVersion")

        val otherDb = await(LocalDatabase.createStoredForTests(encryptionSecret))
        await(otherDb.newQuery[Transaction]().data()) ==> Seq(testTransactionWithId)
        otherDb.getSingletonValue(VersionKey).get ==> "testVersion"
      }
    },
    ManualTest("clear") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        db.addAll(Seq(testTransactionWithId))
        db.setSingletonValue(VersionKey, "testVersion")

        await(db.clear())

        db.isEmpty ==> true
      }
    },
    ManualTest("addAll") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        db.addAll(Seq(testUserRedacted))
        db.addAll(Seq(testTransactionWithId))
        db.addAll(Seq(testTransactionGroupWithId))
        db.addAll(Seq(testBalanceCheckWithId))
        db.addAll(Seq(testExchangeRateMeasurementWithId))

        await(db.newQuery[User]().data()) ==> Seq(testUserRedacted)
        await(db.newQuery[Transaction]().data()) ==> Seq(testTransactionWithId)
        await(db.newQuery[TransactionGroup]().data()) ==> Seq(testTransactionGroupWithId)
        await(db.newQuery[BalanceCheck]().data()) ==> Seq(testBalanceCheckWithId)
        await(db.newQuery[ExchangeRateMeasurement]().data()) ==> Seq(testExchangeRateMeasurementWithId)
      }
    },
    ManualTest("addAll: Inserts no duplicates IDs") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transactionWithSameIdA = testTransactionWithId.copy(categoryCode = "codeA")
        val transactionWithSameIdB = testTransactionWithId.copy(categoryCode = "codeB")
        db.addAll(Seq(testTransactionWithId, transactionWithSameIdA))
        db.addAll(Seq(testTransactionWithId, transactionWithSameIdB))

        await(db.newQuery[Transaction]().data()) ==> Seq(testTransactionWithId)
      }
    },
    ManualTest("applyModifications: Add") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()

        db.applyModifications(Seq(EntityModification.Add(transaction1))) ==> true

        await(db.newQuery[Transaction]().data()) ==> Seq(transaction1)
      }
    },
    ManualTest("applyModifications: Update") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 19191)
        db.addAll(Seq(transaction1))

        db.applyModifications(Seq(EntityModification.createUpdate(updatedTransaction1))) ==> true

        await(db.newQuery[Transaction]().data()) ==> Seq(updatedTransaction1)
      }
    },
    ManualTest("applyModifications: Delete") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        db.addAll(Seq(transaction1))

        db.applyModifications(Seq(EntityModification.createDelete(transaction1))) ==> true

        await(db.newQuery[Transaction]().data()) ==> Seq()
      }
    },
    ManualTest("applyModifications: Add is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        db.applyModifications(
          Seq(
            EntityModification.Add(transaction1),
            EntityModification.Add(transaction1),
            EntityModification.Add(updatedTransaction1),
            EntityModification.Add(transaction2)
          )) ==> true

        await(db.newQuery[Transaction]().data()).toSet ==> Set(transaction1, transaction2)
      }
    },
    ManualTest("applyModifications: Update is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
        val transaction2 = createTransaction()
        db.addAll(Seq(transaction1))

        db.applyModifications(
          Seq(
            EntityModification.Update(updatedTransaction1),
            EntityModification.Update(updatedTransaction1),
            EntityModification.Update(transaction2)
          )) ==> true

        await(db.newQuery[Transaction]().data()) ==> Seq(updatedTransaction1)
      }
    },
    ManualTest("applyModifications: Delete is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()
        db.addAll(Seq(transaction1, transaction2))

        db.applyModifications(
          Seq(
            EntityModification.createDelete(transaction2),
            EntityModification.createDelete(transaction2),
            EntityModification.createDelete(transaction3)
          )) ==> true

        await(db.newQuery[Transaction]().data()) ==> Seq(transaction1)
      }
    },
    ManualTest("applyModifications: Returns false if no change") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        db.applyModifications(Seq(EntityModification.Add(testTransactionWithId))) ==> true

        db.applyModifications(Seq(EntityModification.Add(testTransactionWithId))) ==> false
      }
    }
  )
}
