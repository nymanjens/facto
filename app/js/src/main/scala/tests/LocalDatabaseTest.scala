package tests

import models.access.{DbQuery, DbResultSet, LocalDatabase, ModelField}
import common.testing.TestObjects._
import models.access.LocalDatabase
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.access.webworker.LocalDatabaseWebWorkerApi
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
private[tests] class LocalDatabaseTest extends ManualTestSuite {

  implicit private val webWorker: LocalDatabaseWebWorkerApi =
    new models.access.webworker.Module().localDatabaseWebWorkerApiStub
  private val encryptionSecret = "gA5t6NkQaFpOZsBEU45bZgwlwi7Zeb"

  override def tests = Seq(
    ManualTest("isEmpty") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        await(db.isEmpty) ==> true
        await(db.addAll(Seq(testTransactionWithId)))
        await(db.isEmpty) ==> false

        await(db.resetAndInitialize())

        await(db.isEmpty) ==> true
        await(db.setSingletonValue(NextUpdateTokenKey, testDate))
        await(db.isEmpty) ==> false
      }
    },
    ManualTest("setSingletonValue") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        await(db.getSingletonValue(VersionKey)).isDefined ==> false

        await(db.setSingletonValue(VersionKey, "abc"))
        await(db.getSingletonValue(VersionKey)).get ==> "abc"

        await(db.setSingletonValue(NextUpdateTokenKey, testDate))
        await(db.getSingletonValue(NextUpdateTokenKey)).get ==> testDate
      }
    },
    ManualTest("save") {
      async {
        val db = await(LocalDatabase.createStoredForTests(encryptionSecret))
        await(db.resetAndInitialize())
        await(db.addAll(Seq(testTransactionWithId)))
        await(db.setSingletonValue(VersionKey, "testVersion"))

        await(db.save())
        await(db.setSingletonValue(VersionKey, "otherTestVersion"))

        val otherDb = await(LocalDatabase.createStoredForTests(encryptionSecret))
        await(DbResultSet.fromExecutor(otherDb.queryExecutor[Transaction]()).data()) ==>
          Seq(testTransactionWithId)
        await(otherDb.getSingletonValue(VersionKey)).get ==> "testVersion"
      }
    },
    ManualTest("resetAndInitialize") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        await(db.addAll(Seq(testTransactionWithId)))
        db.setSingletonValue(VersionKey, "testVersion")

        await(db.resetAndInitialize())

        await(db.isEmpty) ==> true
      }
    },
    ManualTest("addAll") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        await(db.addAll(Seq(testUserRedacted)))
        await(db.addAll(Seq(testTransactionWithId)))
        await(db.addAll(Seq(testTransactionGroupWithId)))
        await(db.addAll(Seq(testBalanceCheckWithId)))
        await(db.addAll(Seq(testExchangeRateMeasurementWithId)))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(testUserRedacted)
        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(testTransactionWithId)
        await(DbResultSet.fromExecutor(db.queryExecutor[TransactionGroup]()).data()) ==>
          Seq(testTransactionGroupWithId)
        await(DbResultSet.fromExecutor(db.queryExecutor[BalanceCheck]()).data()) ==>
          Seq(testBalanceCheckWithId)
        await(DbResultSet.fromExecutor(db.queryExecutor[ExchangeRateMeasurement]()).data()) ==>
          Seq(testExchangeRateMeasurementWithId)
      }
    },
    ManualTest("addAll: Inserts no duplicates IDs") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transactionWithSameIdA = testTransactionWithId.copy(categoryCode = "codeA")
        val transactionWithSameIdB = testTransactionWithId.copy(categoryCode = "codeB")
        await(db.addAll(Seq(testTransactionWithId, transactionWithSameIdA)))
        await(db.addAll(Seq(testTransactionWithId, transactionWithSameIdB)))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(testTransactionWithId)
      }
    },
    ManualTest("applyModifications: Add") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()

        await(db.applyModifications(Seq(EntityModification.Add(transaction1)))) ==> true

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(transaction1)
      }
    },
    ManualTest("applyModifications: Update") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 19191)
        await(db.addAll(Seq(transaction1)))

        await(db.applyModifications(Seq(EntityModification.createUpdate(updatedTransaction1)))) ==> true

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(updatedTransaction1)
      }
    },
    ManualTest("applyModifications: Delete") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        await(db.addAll(Seq(transaction1)))

        await(db.applyModifications(Seq(EntityModification.createDelete(transaction1)))) ==> true

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq()
      }
    },
    ManualTest("applyModifications: Add is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        await(
          db.applyModifications(Seq(
            EntityModification.Add(transaction1),
            EntityModification.Add(transaction1),
            EntityModification.Add(updatedTransaction1),
            EntityModification.Add(transaction2)
          ))) ==> true

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()).toSet ==> Set(
          transaction1,
          transaction2)
      }
    },
    ManualTest("applyModifications: Update is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 198237)
        val transaction2 = createTransaction()
        await(db.addAll(Seq(transaction1)))

        await(
          db.applyModifications(
            Seq(
              EntityModification.Update(updatedTransaction1),
              EntityModification.Update(updatedTransaction1),
              EntityModification.Update(transaction2)
            ))) ==> true

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(updatedTransaction1)
      }
    },
    ManualTest("applyModifications: Delete is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()
        await(db.addAll(Seq(transaction1, transaction2)))

        await(
          db.applyModifications(
            Seq(
              EntityModification.createDelete(transaction2),
              EntityModification.createDelete(transaction2),
              EntityModification.createDelete(transaction3)
            ))) ==> true

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(transaction1)
      }
    },
    ManualTest("applyModifications: Returns false if no change") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests(encryptionSecret))
        await(db.applyModifications(Seq(EntityModification.Add(testTransactionWithId)))) ==> true

        await(db.applyModifications(Seq(EntityModification.Add(testTransactionWithId)))) ==> false
      }
    }
  )
}
