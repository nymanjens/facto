package tests

import app.common.testing.TestObjects._
import app.models.access.ModelFields
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

  override def tests =
    testsWithParameters(separateDbPerCollection = false) ++
      testsWithParameters(separateDbPerCollection = true)

  private def testsWithParameters(separateDbPerCollection: Boolean) = {
    def manualTest(name: String)(testCode: => Future[Unit]): ManualTest =
      ManualTest(s"$name [separateDbPerCollection = $separateDbPerCollection]")(testCode)

    Seq(
      manualTest("isEmpty") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.isEmpty) ==> true
          await(db.addAll(Seq(createUser())))
          await(db.isEmpty) ==> false

          await(db.resetAndInitialize())

          await(db.isEmpty) ==> true
          await(db.setSingletonValue(NextUpdateTokenKey, testUpdateToken))
          await(db.isEmpty) ==> false
        }
      },
      manualTest("setSingletonValue") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.getSingletonValue(VersionKey)).isDefined ==> false

          await(db.setSingletonValue(VersionKey, "abc")) ==> true
          await(db.getSingletonValue(VersionKey)).get ==> "abc"

          await(db.setSingletonValue(VersionKey, "abc")) ==> false
          await(db.getSingletonValue(VersionKey)).get ==> "abc"

          await(db.setSingletonValue(VersionKey, "def")) ==> true
          await(db.getSingletonValue(VersionKey)).get ==> "def"

          await(db.setSingletonValue(NextUpdateTokenKey, testUpdateToken)) ==> true
          await(db.getSingletonValue(NextUpdateTokenKey)).get ==> testUpdateToken
        }
      },
      manualTest("addSingletonValueIfNew") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.getSingletonValue(VersionKey)).isDefined ==> false

          await(db.addSingletonValueIfNew(VersionKey, "abc")) ==> true
          await(db.getSingletonValue(VersionKey)).get ==> "abc"

          await(db.addSingletonValueIfNew(VersionKey, "abc")) ==> false
          await(db.getSingletonValue(VersionKey)).get ==> "abc"

          await(db.addSingletonValueIfNew(VersionKey, "def")) ==> false
          await(db.getSingletonValue(VersionKey)).get ==> "abc"
        }
      },
      manualTest("resetAndInitialize") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.addAll(Seq(createUser())))
          db.setSingletonValue(VersionKey, "testVersion")

          await(db.resetAndInitialize())

          await(db.isEmpty) ==> true
        }
      },
      manualTest("addAll") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.addAll(Seq(testUserRedacted))) ==> true
          await(db.addAll(Seq(testUserRedacted))) ==> false
          await(db.addAll(Seq(testTransactionWithId))) ==> true
          await(db.addAll(Seq(testBalanceCheckWithId))) ==> true
          await(db.addAll(Seq(testExchangeRateMeasurementWithId))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(testUserRedacted)
          await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(
            testTransactionWithId)
          await(DbResultSet.fromExecutor(db.queryExecutor[BalanceCheck]()).data()) ==>
            Seq(testBalanceCheckWithId)
          await(DbResultSet.fromExecutor(db.queryExecutor[ExchangeRateMeasurement]()).data()) ==>
            Seq(testExchangeRateMeasurementWithId)
        }
      },
      manualTest("addAll: Inserts no duplicates IDs") {
        async {
          val user1 = createUser()

          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          val userWithSameIdA = user1.copy(name = "name A")
          val userWithSameIdB = user1.copy(name = "name B")
          await(db.addAll(Seq(user1, userWithSameIdA))) ==> true
          await(db.addAll(Seq(user1, userWithSameIdB))) ==> false

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
        }
      },
      manualTest("addPendingModifications") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))

          await(db.addPendingModifications(Seq(testModificationA, testModificationB))) ==> true
          await(db.addPendingModifications(Seq(testModificationB))) ==> false

          await(db.pendingModifications()) ==> Seq(testModificationA, testModificationB)
        }
      },
      manualTest("removePendingModifications: Modification in db") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.addPendingModifications(Seq(testModificationA)))
          await(db.addPendingModifications(Seq(testModificationB)))

          await(db.removePendingModifications(Seq(testModificationA))) ==> true

          await(db.pendingModifications()) ==> Seq(testModificationB)
        }
      },
      manualTest("removePendingModifications: Modification not in db") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          await(db.addPendingModifications(Seq(testModificationA)))

          await(db.removePendingModifications(Seq(testModificationB))) ==> false

          await(db.pendingModifications()) ==> Seq(testModificationA)
        }
      },
      manualTest("applyModifications: Add") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          val user1 = createUser()

          await(db.applyModifications(Seq(EntityModification.Add(user1)))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)

          await(db.applyModifications(Seq(EntityModification.Add(user1)))) ==> false
        }
      },
      manualTest("applyModifications: Update: Full update") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          val user1 = createUser()
          val user2 = createUser()
          val user3 = createUser()

          await(db.addAll(Seq(user1, user2, user3)))

          val user2Update = EntityModification.createUpdateAllFields(user2.copy(name = "other name"))
          await(db.applyModifications(Seq(user2Update))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()).toSet ==>
            Set(user1, user2Update.updatedEntity, user3)
        }
      },
      manualTest("applyModifications: Update: Partial update") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          val user1 = createUser()
          val user2 = createUser()
          val user3 = createUser()

          await(db.addAll(Seq(user1, user2, user3)))

          val user2UpdateA = EntityModification
            .createUpdate(
              user2.copy(loginName = "login2_update"),
              fieldMask = Seq(ModelFields.User.loginName))
          val user2UpdateB = EntityModification
            .createUpdate(user2.copy(name = "name2_update"), fieldMask = Seq(ModelFields.User.name))
          await(db.applyModifications(Seq(user2UpdateA))) ==> true
          await(db.applyModifications(Seq(user2UpdateB))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()).toSet ==>
            Set(
              user1,
              user2.copy(
                loginName = "login2_update",
                name = "name2_update",
                lastUpdateTime = user2UpdateA.updatedEntity.lastUpdateTime
                  .merge(user2UpdateB.updatedEntity.lastUpdateTime, forceIncrement = false)
              ),
              user3
            )
        }
      },
      manualTest("applyModifications: Update: Ignored when already deleted") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          val user1 = createUser()
          val user2 = createUser()
          val user3 = createUser()

          await(db.addAll(Seq(user1, user3)))

          val user2Update = EntityModification.createUpdateAllFields(user2)
          await(db.applyModifications(Seq(user2Update))) ==> false

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()).toSet ==> Set(user1, user3)
        }
      },
      manualTest("applyModifications: Delete") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
          val user1 = createUser()
          await(db.addAll(Seq(user1)))

          await(db.applyModifications(Seq(EntityModification.createRemove(user1)))) ==> true
          await(db.applyModifications(Seq(EntityModification.createRemove(user1)))) ==> false

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq()
        }
      },
      manualTest("applyModifications: Add is idempotent") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
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
              ))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()).toSet ==> Set(user1, user2)
        }
      },
      manualTest("applyModifications: Update is idempotent") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))

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
              ))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(updatedUserB)
        }
      },
      manualTest("applyModifications: Delete is idempotent") {
        async {
          val db = await(createAndInitializeDb(separateDbPerCollection = separateDbPerCollection))
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
              ))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
        }
      },
    )
  }

  def createAndInitializeDb(separateDbPerCollection: Boolean): Future[LocalDatabase] = async {
    val db =
      await(LocalDatabaseImpl.createInMemoryForTests(separateDbPerCollection = separateDbPerCollection))
    await(db.resetAndInitialize())
    db
  }

  private def createUser(): User =
    testUserRedacted.copy(idOption = Some(EntityModification.generateRandomId()))
}
