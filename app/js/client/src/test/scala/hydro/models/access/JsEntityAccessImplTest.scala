package hydro.models.access

import app.common.testing.FakeScalaJsApiClient
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.models.modification.EntityTypes
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.common.testing.ModificationsBuffer
import hydro.common.testing.FakeLocalDatabase
import hydro.common.time.Clock
import hydro.models.Entity
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object JsEntityAccessImplTest extends TestSuite {

  override def tests = TestSuite {
    implicit val fakeApiClient: FakeScalaJsApiClient = new FakeScalaJsApiClient()
    implicit val fakeClock: Clock = new TestModule().fakeClock
    implicit val entitySyncLogic: EntitySyncLogic = new EntitySyncLogic.FullySynced(EntityTypes.all)
    implicit val getInitialDataResponse = testGetInitialDataResponse
    implicit val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
      new HydroPushSocketClientFactory
    val fakeLocalDatabase: FakeLocalDatabase = new FakeLocalDatabase()
    val localDatabasePromise: Promise[LocalDatabase] = Promise()
    implicit val remoteDatabaseProxy: HybridRemoteDatabaseProxy =
      HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
    val entityAccess = new JsEntityAccessImpl()

    "Fake local database not yet loaded" - {
      "newQuery" - async {
        fakeApiClient.addEntities(testUser)

        await(entityAccess.newQuery[User]().data()) ==> Seq(testUser)
      }

      "persistModifications()" - async {
        await(entityAccess.persistModifications(Seq(testModification)))

        fakeApiClient.allModifications ==> Seq(testModification)
      }

      "persistModifications(): calls listeners" - async {
        val listener = new FakeProxyListener()
        entityAccess.registerListener(listener)

        await(entityAccess.persistModifications(Seq(testModification)))

        listener.modifications ==> Seq(Seq(testModification))
      }
    }

    "Fake local database loaded" - {
      "loads initial data if db is empty" - async {
        await(fakeApiClient.persistEntityModifications(Seq(testModification)))
        localDatabasePromise.success(fakeLocalDatabase)
        await(remoteDatabaseProxy.localDatabaseReadyFuture)

        fakeLocalDatabase.allModifications ==> Seq(testModification)
      }

      "loads initial data if db is non-empty but has wrong version" - async {
        fakeLocalDatabase.applyModifications(Seq(testModificationA))
        fakeApiClient.persistEntityModifications(Seq(testModificationB))
        localDatabasePromise.success(fakeLocalDatabase)
        await(remoteDatabaseProxy.localDatabaseReadyFuture)

        fakeLocalDatabase.allModifications ==> Seq(testModificationB)
      }

      "does not load initial data if db is non-empty with right version" - async {
        fakeApiClient.persistEntityModifications(Seq(testModificationA))
        localDatabasePromise.success(fakeLocalDatabase)

        val entityAccess1 = {
          implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
          await(remoteDatabaseProxy.localDatabaseReadyFuture)
          new JsEntityAccessImpl()
        }
        fakeApiClient.persistEntityModifications(Seq(testModificationB))

        val entityAccess2 = {
          implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
          await(remoteDatabaseProxy.localDatabaseReadyFuture)
          new JsEntityAccessImpl()
        }

        fakeLocalDatabase.allModifications ==> Seq(testModificationA)
      }
    }
  }

  private final class FakeProxyListener extends JsEntityAccess.Listener {
    private val _modifications: mutable.Buffer[Seq[EntityModification]] = mutable.Buffer()

    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]) = {
      _modifications += modifications
    }

    def modifications: Seq[Seq[EntityModification]] = _modifications.toVector
  }
}
