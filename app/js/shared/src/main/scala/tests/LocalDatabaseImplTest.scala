package tests

import app.common.testing.TestObjects._
import app.models.access.ModelFields
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.testing.Awaiter
import hydro.common.testing.FakeClock
import hydro.models.access.DbResultSet
import hydro.models.access.LocalDatabase
import hydro.models.access.LocalDatabaseImpl
import hydro.models.access.SingletonKey.NextUpdateTokenKey
import hydro.models.access.SingletonKey.VersionKey
import hydro.models.modification.EntityModification
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.access.SingletonKey.DbStatus
import hydro.models.access.SingletonKey.DbStatusKey
import hydro.models.access.webworker.LocalDatabaseWebWorkerApiStub
import hydro.models.access.worker.JsWorkerClientFacade
import hydro.models.access.PendingModificationsListener
import tests.ManualTests.ManualTest
import tests.ManualTests.ManualTestSuite

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] class LocalDatabaseImplTest extends ManualTestSuite {

  private val testDbStatusA = DbStatus.Populating(testInstantA)
  private val testDbStatusB = DbStatus.Populating(testInstantB)
  private val testDbStatusC = DbStatus.Populating(testInstantC)

  implicit private val secondaryIndexFunction = app.models.access.Module.secondaryIndexFunction
  implicit private val fakeClock = new FakeClock

  override def tests =
    Seq(
      testsWithParameters(
        TestParameters(
          separateDbPerCollection = false,
          jsWorker = JsWorkerClientFacade.getSharedIfSupported().get,
        )
      ),
      testsWithParameters(
        TestParameters(
          separateDbPerCollection = true,
          jsWorker = JsWorkerClientFacade.getSharedIfSupported().get,
        )
      ),
      testsWithParameters(
        TestParameters(
          separateDbPerCollection = false,
          jsWorker = JsWorkerClientFacade.getDedicated(),
        )
      ),
    ).flatten

  private def testsWithParameters(testParameters: TestParameters) = {
    def manualTest(name: String)(testCode: => Future[Unit]): ManualTest =
      ManualTest(
        s"$name " +
          s"[separateDbPerCollection=${testParameters.separateDbPerCollection}, " +
          s"${testParameters.jsWorker.getClass.getSimpleName}]"
      )(testCode)

    Seq(
      manualTest("isEmpty") {
        async {
          val db = await(createAndInitializeDb(testParameters))
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
          val db = await(createAndInitializeDb(testParameters))
          await(db.getSingletonValue(DbStatusKey)).isDefined ==> false

          await(db.setSingletonValue(DbStatusKey, testDbStatusA)) ==> true
          await(db.getSingletonValue(DbStatusKey)).get ==> testDbStatusA

          await(db.setSingletonValue(DbStatusKey, testDbStatusA)) ==> false
          await(db.getSingletonValue(DbStatusKey)).get ==> testDbStatusA

          await(db.setSingletonValue(DbStatusKey, testDbStatusB)) ==> true
          await(db.getSingletonValue(DbStatusKey)).get ==> testDbStatusB

          await(db.setSingletonValue(NextUpdateTokenKey, testUpdateToken)) ==> true
          await(db.getSingletonValue(NextUpdateTokenKey)).get ==> testUpdateToken
        }
      },
      manualTest("setSingletonValue(abortUnlessExistingValueEquals)") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          await(db.getSingletonValue(DbStatusKey)).isDefined ==> false

          await(
            db.setSingletonValue(DbStatusKey, testDbStatusA, abortUnlessExistingValueEquals = testDbStatusC)
          ) ==> false
          await(db.getSingletonValue(DbStatusKey)).isDefined ==> false

          await(db.setSingletonValue(DbStatusKey, testDbStatusA)) ==> true
          await(
            db.setSingletonValue(DbStatusKey, testDbStatusA, abortUnlessExistingValueEquals = testDbStatusA)
          ) ==> false
          await(db.getSingletonValue(DbStatusKey)).get ==> testDbStatusA

          await(
            db.setSingletonValue(DbStatusKey, testDbStatusB, abortUnlessExistingValueEquals = testDbStatusC)
          ) ==> false
          await(db.getSingletonValue(DbStatusKey)).get ==> testDbStatusA

          await(
            db.setSingletonValue(DbStatusKey, testDbStatusB, abortUnlessExistingValueEquals = testDbStatusA)
          ) ==> true
          await(db.getSingletonValue(DbStatusKey)).get ==> testDbStatusB
        }
      },
      manualTest("addSingletonValueIfNew") {
        async {
          val db = await(createAndInitializeDb(testParameters))
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
          val db = await(createAndInitializeDb(testParameters))
          await(db.addAll(Seq(createUser())))
          db.setSingletonValue(VersionKey, "testVersion")

          await(db.resetAndInitialize())

          await(db.isEmpty) ==> true
        }
      },
      manualTest("resetAndInitialize(alsoSetSingleton)") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          await(db.addAll(Seq(createUser())))
          db.setSingletonValue(VersionKey, "testVersion")

          await(db.resetAndInitialize(alsoSetSingleton = (VersionKey, "abc")))

          await(db.getSingletonValue(VersionKey)) ==> Some("abc")
        }
      },
      manualTest("addAll") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          await(db.addAll(Seq(testUserRedacted))) ==> true
          await(db.addAll(Seq(testUserRedacted))) ==> false
          await(db.addAll(Seq(testTransactionWithId))) ==> true
          await(db.addAll(Seq(testBalanceCheckWithId))) ==> true
          await(db.addAll(Seq(testExchangeRateMeasurementWithId))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(testUserRedacted)
          await(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()).data()) ==> Seq(
            testTransactionWithId
          )
          await(DbResultSet.fromExecutor(db.queryExecutor[BalanceCheck]()).data()) ==>
            Seq(testBalanceCheckWithId)
          await(DbResultSet.fromExecutor(db.queryExecutor[ExchangeRateMeasurement]()).data()) ==>
            Seq(testExchangeRateMeasurementWithId)
        }
      },
      manualTest("addAll: Inserts no duplicates IDs") {
        async {
          val user1 = createUser()

          val db = await(createAndInitializeDb(testParameters))
          val userWithSameIdA = user1.copy(name = "name A")
          val userWithSameIdB = user1.copy(name = "name B")
          await(db.addAll(Seq(user1, userWithSameIdA))) ==> true
          await(db.addAll(Seq(user1, userWithSameIdB))) ==> false

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
        }
      },
      manualTest("addPendingModifications") {
        async {
          val db = await(createAndInitializeDb(testParameters))

          await(db.addPendingModifications(Seq(testModificationA, testModificationB))) ==> true
          await(db.addPendingModifications(Seq(testModificationB))) ==> false

          await(db.pendingModifications()) ==> Seq(testModificationA, testModificationB)
        }
      },
      manualTest("removePendingModifications: Modification in db") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          await(db.addPendingModifications(Seq(testModificationA)))
          await(db.addPendingModifications(Seq(testModificationB)))

          await(db.removePendingModifications(Seq(testModificationA))) ==> true

          await(db.pendingModifications()) ==> Seq(testModificationB)
        }
      },
      manualTest("removePendingModifications: Modification not in db") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          await(db.addPendingModifications(Seq(testModificationA)))

          await(db.removePendingModifications(Seq(testModificationB))) ==> false

          await(db.pendingModifications()) ==> Seq(testModificationA)
        }
      },
      manualTest("applyModifications: Add") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          val user1 = createUser()

          await(db.applyModifications(Seq(EntityModification.Add(user1)))) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)

          await(db.applyModifications(Seq(EntityModification.Add(user1)))) ==> false
        }
      },
      manualTest("applyModifications: Update: Full update") {
        async {
          val db = await(createAndInitializeDb(testParameters))
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
          val db = await(createAndInitializeDb(testParameters))
          val user1 = createUser()
          val user2 = createUser()
          val user3 = createUser()

          await(db.addAll(Seq(user1, user2, user3)))

          val user2UpdateA = EntityModification
            .createUpdate(
              user2.copy(loginName = "login2_update"),
              fieldMask = Seq(ModelFields.User.loginName),
            )
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
                  .merge(user2UpdateB.updatedEntity.lastUpdateTime, forceIncrement = false),
              ),
              user3,
            )
        }
      },
      manualTest("applyModifications: Update: Ignored when already deleted") {
        async {
          val db = await(createAndInitializeDb(testParameters))
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
          val db = await(createAndInitializeDb(testParameters))
          val user1 = createUser()
          await(db.addAll(Seq(user1)))

          await(db.applyModifications(Seq(EntityModification.createRemove(user1)))) ==> true
          await(db.applyModifications(Seq(EntityModification.createRemove(user1)))) ==> false

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq()
        }
      },
      manualTest("applyModifications: Add is idempotent") {
        async {
          val db = await(createAndInitializeDb(testParameters))
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
                EntityModification.Add(user2),
              )
            )
          ) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()).toSet ==> Set(user1, user2)
        }
      },
      manualTest("applyModifications: Update is idempotent") {
        async {
          val db = await(createAndInitializeDb(testParameters))

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
              )
            )
          ) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(updatedUserB)
        }
      },
      manualTest("applyModifications: Delete is idempotent") {
        async {
          val db = await(createAndInitializeDb(testParameters))
          val user1 = createUser()
          val user2 = createUser()
          val user3 = createUser()
          await(db.addAll(Seq(user1, user2)))

          await(
            db.applyModifications(
              Seq(
                EntityModification.createRemove(user2),
                EntityModification.createRemove(user2),
                EntityModification.createRemove(user3),
              )
            )
          ) ==> true

          await(DbResultSet.fromExecutor(db.queryExecutor[User]()).data()) ==> Seq(user1)
        }
      },
      manualTest("registerPendingModificationsListener: Listener is not called for same instance") {
        async {
          val db = await(createAndInitializeDb(testParameters))

          val nextEventPromise: Promise[EntityModification] = Promise()
          db.registerPendingModificationsListener(new PendingModificationsListener {
            override def onPendingModificationAddedByOtherInstance(modification: EntityModification): Unit =
              nextEventPromise.tryFailure(new AssertionError(s"Got Add($modification)"))
            override def onPendingModificationRemovedByOtherInstance(
                modificationPseudoUniqueIdentifier: Long
            ): Unit =
              nextEventPromise.tryFailure(
                new AssertionError(s"Got Remove($modificationPseudoUniqueIdentifier)")
              )
          })

          await(db.addPendingModifications(Seq(testModificationA, testModificationB))) ==> true
          await(db.removePendingModifications(Seq(testModificationB))) ==> true

          await(db.pendingModifications()) ==> Seq(testModificationA)
          await(Awaiter.expectConsistently.neverComplete(nextEventPromise.future))
        }
      },
      manualTest("registerPendingModificationsListener: Listener is called for different instance") {
        async {
          if (testParameters.jsWorker == JsWorkerClientFacade.getSharedIfSupported().get) {
            val db = await(createAndInitializeDb(testParameters))

            val receivedAdditions: mutable.Buffer[EntityModification] = mutable.Buffer()
            val errors: mutable.Buffer[Throwable] = mutable.Buffer()
            db.registerPendingModificationsListener(new PendingModificationsListener {
              override def onPendingModificationAddedByOtherInstance(modification: EntityModification): Unit =
                receivedAdditions += modification
              override def onPendingModificationRemovedByOtherInstance(
                  modificationPseudoUniqueIdentifier: Long
              ): Unit =
                errors += new AssertionError(s"Got Remove($modificationPseudoUniqueIdentifier)")
            })

            val otherDb = {
              implicit val webWorker =
                new LocalDatabaseWebWorkerApiStub(forceJsWorker = Some(testParameters.jsWorker))
              await(
                LocalDatabaseImpl.createInMemoryForTests(
                  separateDbPerCollection = testParameters.separateDbPerCollection
                )
              )
            }

            await(otherDb.addPendingModifications(Seq(testModificationA))) ==> true
            await(otherDb.addPendingModifications(Seq(testModificationA))) ==> false

            await(db.pendingModifications()) ==> Seq(testModificationA)
            await(Awaiter.expectConsistently.isEmpty(errors))
            await(Awaiter.expectEventually.nonEmpty(receivedAdditions))
            await(Awaiter.expectConsistently.equal(receivedAdditions.toVector, Seq(testModificationA)))
          }
        }
      },
    )
  }

  private def createAndInitializeDb(
      testParameters: TestParameters
  ): Future[LocalDatabase] = async {
    implicit val webWorker = new LocalDatabaseWebWorkerApiStub(forceJsWorker = Some(testParameters.jsWorker))

    val db =
      await(
        LocalDatabaseImpl.createInMemoryForTests(
          separateDbPerCollection = testParameters.separateDbPerCollection
        )
      )
    await(db.resetAndInitialize())
    db
  }

  private def createUser(): User = {
    testUserRedacted.copy(idOption = Some(EntityModification.generateRandomId()))
  }

  case class TestParameters(
      separateDbPerCollection: Boolean,
      jsWorker: JsWorkerClientFacade,
  )
}
