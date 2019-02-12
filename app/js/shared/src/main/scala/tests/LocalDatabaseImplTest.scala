package tests

import app.common.testing.TestObjects._
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import hydro.models.modification.EntityModification
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.testing.FakeClock
import hydro.models.access.DbResultSet
import hydro.models.access.LocalDatabase
import hydro.models.access.LocalDatabaseImpl
import hydro.models.access.SingletonKey.NextUpdateTokenKey
import hydro.models.access.SingletonKey.VersionKey
import hydro.models.UpdatableEntity.LastUpdateTime
import tests.ManualTests.ManualTest
import tests.ManualTests.ManualTestSuite

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] class LocalDatabaseImplTest extends ManualTestSuite {

  implicit private val webWorker = new hydro.models.access.webworker.Module().localDatabaseWebWorkerApiStub
  implicit private val secondaryIndexFunction = app.models.access.Module.secondaryIndexFunction
  implicit private val fakeClock = new FakeClock

  override def tests = Seq(
    ManualTest("isEmpty") {
      async {
        val db = await(createAndInitializeDb())
        await(db.isEmpty) ==> true
        await(db.addAll(Seq(createUser())))
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
        val user1 = createUser()

        val db = await(LocalDatabaseImpl.createStoredForTests())
        await(db.resetAndInitialize())
        await(db.addAll(Seq(user1)))
        await(db.setSingletonValue(VersionKey, "testVersion"))

        await(db.save())
        await(db.setSingletonValue(VersionKey, "otherTestVersion"))

        val otherDb = await(LocalDatabaseImpl.createStoredForTests())
        await(DbResultSet.fromExecutor(otherDb.queryExecutor[User]()).data()) ==> Seq(user1)
        await(otherDb.getSingletonValue(VersionKey)).get ==> "testVersion"
      }
    },
    ManualTest("resetAndInitialize") {
      async {
        val db = await(createAndInitializeDb())
        await(db.addAll(Seq(createUser())))
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
        await(db.addAll(Seq(testBalanceCheckWithId)))
        await(db.addAll(Seq(testExchangeRateMeasurementWithId)))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(testUserRedacted)
        await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(testTransactionWithId)
        await(DbResultSet.fromExecutor(db.queryExecutor[BalanceCheck]()).data()) ==>
          Seq(testBalanceCheckWithId)
        await(DbResultSet.fromExecutor(db.queryExecutor[ExchangeRateMeasurement]()).data()) ==>
          Seq(testExchangeRateMeasurementWithId)
      }
    },
    ManualTest("addAll: Inserts no duplicates IDs") {
      async {
        val user1 = createUser()

        val db = await(createAndInitializeDb())
        val userWithSameIdA = user1.copy(name = "name A")
        val userWithSameIdB = user1.copy(name = "name B")
        await(db.addAll(Seq(user1, userWithSameIdA)))
        await(db.addAll(Seq(user1, userWithSameIdB)))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
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
        val user1 = createUser()

        await(db.applyModifications(Seq(EntityModification.Add(user1))))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
      }
    },
    ManualTest("applyModifications: Update") {
      async {
        val db = await(createAndInitializeDb())
        val user1 = createUser()
        val userUpdate = EntityModification.createUpdateAllFields(user1.copy(name = "updated name"))
        await(db.addAll(Seq(user1)))

        await(db.applyModifications(Seq(userUpdate)))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(userUpdate.updatedEntity)
      }
    },
    ManualTest("applyModifications: Delete") {
      async {
        val db = await(createAndInitializeDb())
        val user1 = createUser()
        await(db.addAll(Seq(user1)))

        await(db.applyModifications(Seq(EntityModification.createRemove(user1))))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq()
      }
    },
    ManualTest("applyModifications: Add is idempotent") {
      async {
        val db = await(createAndInitializeDb())
        val user1 = createUser()
        val updatedUser1 = user1.copy(name = "updated name")
        val user2 = createUser()
        val user3 = createUser()

        await(
          db.applyModifications(
            Seq(
              EntityModification.Add(user1),
              EntityModification.Add(user1),
              EntityModification.Add(updatedUser1),
              EntityModification.Add(user2)
            )))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()).toSet ==> Set(user1, user2)
      }
    },
    ManualTest("applyModifications: Update is idempotent") {
      async {
        val db = await(createAndInitializeDb())

        val user1 = createUser()
        val updatedUserA =
          user1.copy(name = "A", lastUpdateTime = LastUpdateTime.allFieldsUpdated(testInstantA))
        val updatedUserB =
          user1.copy(name = "B", lastUpdateTime = LastUpdateTime.allFieldsUpdated(testInstantB))
        await(db.applyModifications(Seq(EntityModification.Add(user1))))

        await(
          db.applyModifications(
            Seq(
              EntityModification.Update(updatedUserB),
              EntityModification.Update(updatedUserA),
              EntityModification.Update(updatedUserB),
            )))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(updatedUserB)
      }
    },
    ManualTest("applyModifications: Delete is idempotent") {
      async {
        val db = await(createAndInitializeDb())
        val user1 = createUser()
        val user2 = createUser()
        val user3 = createUser()
        await(db.addAll(Seq(user1, user2)))

        await(
          db.applyModifications(
            Seq(
              EntityModification.createRemove(user2),
              EntityModification.createRemove(user2),
              EntityModification.createRemove(user3)
            )))

        await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
      }
    },
  )

  def createAndInitializeDb(): Future[LocalDatabase] = async {
    val db = await(LocalDatabaseImpl.createInMemoryForTests())
    await(db.resetAndInitialize())
    db
  }

  private def createUser(): User =
    testUserRedacted.copy(idOption = Some(EntityModification.generateRandomId()))
}
