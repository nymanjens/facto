package tests

import app.common.testing.TestObjects._
import hydro.models.access.SingletonKey.NextUpdateTokenKey
import hydro.models.access.SingletonKey.VersionKey
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi
import hydro.models.access.DbResultSet
import hydro.models.access.LocalDatabase
import hydro.models.access.LocalDatabaseImpl
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.modification.EntityModification
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import tests.ManualTests.ManualTest
import tests.ManualTests.ManualTestSuite

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import hydro.scala2js.StandardConverters._
import app.scala2js.AppConverters._
import hydro.models.access.DbResultSet
import hydro.models.access.DbQueryExecutor
import hydro.models.access.DbQuery

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] class LocalDatabaseTest extends ManualTestSuite {

  implicit private val webWorker = new hydro.models.access.webworker.Module().localDatabaseWebWorkerApiStub
  implicit private val secondaryIndexFunction = app.models.access.Module.secondaryIndexFunction

  override def tests = Seq(
    ManualTest("isEmpty") {
      async {
        val db = await(createAndInitializeDb())
        await(db.isEmpty) ==> true
        await(db.addAll(Seq(testTransactionWithId)))
        await(db.isEmpty) ==> false

        await(db.resetAndInitialize())

        await(db.isEmpty) ==> true
        await(db.setSingletonValue(NextUpdateTokenKey, testUpdateToken))
        await(db.isEmpty) ==> false
      }
    },
    ManualTest("setSingletonValue") {
      async {
        val db = await(createAndInitializeDb())
        await(db.getSingletonValue(VersionKey)).isDefined ==> false

        await(db.setSingletonValue(VersionKey, "abc"))
        await(db.getSingletonValue(VersionKey)).get ==> "abc"

        await(db.setSingletonValue(NextUpdateTokenKey, testUpdateToken))
        await(db.getSingletonValue(NextUpdateTokenKey)).get ==> testUpdateToken
      }
    },
    ManualTest("save") {
      async {
        val db = await(LocalDatabaseImpl.createStoredForTests())
        await(db.resetAndInitialize())
        await(db.addAll(Seq(testTransactionWithId)))
        await(db.setSingletonValue(VersionKey, "testVersion"))

        await(db.save())
        await(db.setSingletonValue(VersionKey, "otherTestVersion"))

        val otherDb = await(LocalDatabaseImpl.createStoredForTests())
        await(DbResultSet.fromExecutor(otherDb.queryExecutor[Transaction]()).data()) ==>
          Seq(testTransactionWithId)
        await(otherDb.getSingletonValue(VersionKey)).get ==> "testVersion"
      }
    },
    ManualTest("resetAndInitialize") {
      async {
        val db = await(createAndInitializeDb())
        await(db.addAll(Seq(testTransactionWithId)))
        db.setSingletonValue(VersionKey, "testVersion")

        await(db.resetAndInitialize())

        await(db.isEmpty) ==> true
      }
    },
    ManualTest("addAll") {
      async {
        val db = await(createAndInitializeDb())
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
        val db = await(createAndInitializeDb())
        val transactionWithSameIdA = testTransactionWithId.copy(categoryCode = "codeA")
        val transactionWithSameIdB = testTransactionWithId.copy(categoryCode = "codeB")
        await(db.addAll(Seq(testTransactionWithId, transactionWithSameIdA)))
        await(db.addAll(Seq(testTransactionWithId, transactionWithSameIdB)))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(testTransactionWithId)
      }
    },
    ManualTest("addPendingModifications") {
      async {
        val db = await(createAndInitializeDb())

        await(db.addPendingModifications(Seq(testModificationA, testModificationB)))
        await(db.addPendingModifications(Seq(testModificationB)))

        await(db.pendingModifications()) ==> Seq(testModificationA, testModificationB)
      }
    },
    ManualTest("removePendingModifications: Modification in db") {
      async {
        val db = await(createAndInitializeDb())
        await(db.addPendingModifications(Seq(testModificationA)))
        await(db.addPendingModifications(Seq(testModificationB)))

        await(db.removePendingModifications(Seq(testModificationA)))

        await(db.pendingModifications()) ==> Seq(testModificationB)
      }
    },
    ManualTest("removePendingModifications: Modification not in db") {
      async {
        val db = await(createAndInitializeDb())
        await(db.addPendingModifications(Seq(testModificationA)))

        await(db.removePendingModifications(Seq(testModificationB)))

        await(db.pendingModifications()) ==> Seq(testModificationA)
      }
    },
    ManualTest("applyModifications: Add") {
      async {
        val db = await(createAndInitializeDb())
        val transaction1 = createTransaction()

        await(db.applyModifications(Seq(EntityModification.Add(transaction1))))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(transaction1)
      }
    },
    ManualTest("applyModifications: Update") {
      async {
        val db = await(createAndInitializeDb())
        val transaction1 = createTransaction()
        val updatedTransaction1 = transaction1.copy(flowInCents = 19191)
        await(db.addAll(Seq(transaction1)))

        await(db.applyModifications(Seq(EntityModification.createUpdate(updatedTransaction1))))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(updatedTransaction1)
      }
    },
    ManualTest("applyModifications: Delete") {
      async {
        val db = await(createAndInitializeDb())
        val transaction1 = createTransaction()
        await(db.addAll(Seq(transaction1)))

        await(db.applyModifications(Seq(EntityModification.createDelete(transaction1))))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq()
      }
    },
    ManualTest("applyModifications: Add is idempotent") {
      async {
        val db = await(createAndInitializeDb())
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
          )))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()).toSet ==> Set(
          transaction1,
          transaction2)
      }
    },
    ManualTest("applyModifications: Update is idempotent") {
      async {
        val db = await(createAndInitializeDb())
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
            )))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(updatedTransaction1)
      }
    },
    ManualTest("applyModifications: Delete is idempotent") {
      async {
        val db = await(createAndInitializeDb())
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
            )))

        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(transaction1)
      }
    }
  )

  def createAndInitializeDb(): Future[LocalDatabase] = async {
    val db = await(LocalDatabaseImpl.createInMemoryForTests())
    await(db.resetAndInitialize())
    db
  }
}
