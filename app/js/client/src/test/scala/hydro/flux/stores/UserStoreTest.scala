package hydro.flux.stores

import app.api.ScalaJsApi.UserPrototype
import app.common.testing.TestObjects._
import hydro.common.testing.Awaiter
import hydro.flux.action.StandardActions
import hydro.models.modification.EntityModification
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

// Also tests `AsyncEntityDerivedStateStore`
object UserStoreTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new app.common.testing.TestModule
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val fakeDispatcher = testModule.fakeDispatcher
    implicit val fakeScalaJsApiClient = testModule.fakeScalaJsApiClient
    implicit val fakeClock = testModule.fakeClock

    val store: UserStore = new UserStore()

    "Listens to UpsertUser" - async {
      val userPrototype = UserPrototype.create(id = 19283L)

      await(fakeDispatcher.dispatch(StandardActions.UpsertUser(userPrototype)))

      fakeScalaJsApiClient.allUpsertedUserPrototypes ==> Seq(userPrototype)
    }

    "Registers callback at dispatcher" - {
      fakeDispatcher.callbacks.size ==> 1
    }

    "store state is updated upon remote update" - async {
      await(store.stateFuture) ==> UserStore.State(allUsers = Seq())

      entityAccess.addRemotelyAddedEntities(testUserA)

      await(Awaiter.expectEventually.nonEmpty(store.state.get.allUsers))
      store.state.get.allUsers ==> Seq(testUserA)
    }

    "store calls listeners" - async {
      var onStateUpdateCount = 0
      store.register(() => {
        onStateUpdateCount += 1
      })

      await(Awaiter.expectEventually.equal(onStateUpdateCount, 1))

      entityAccess.addRemotelyAddedEntities(testUserA)

      await(Awaiter.expectEventually.equal(onStateUpdateCount, 2))

      entityAccess.addRemotelyAddedEntities(testUserA) // Duplicate

      await(Awaiter.expectConsistently.equal(onStateUpdateCount, 2))
    }

    "store copes with update during recalculation" - async {
      val testUserBUpdate = EntityModification.createUpdateAllFields(testUserB.copy(name = "other name"))
      entityAccess.slowDownQueries(50.milliseconds)

      entityAccess.addRemotelyAddedEntities(testUserA)
      entityAccess.addRemotelyAddedEntities(testUserB)
      val newStateFuture = store.stateFuture
      entityAccess.persistModifications(testUserBUpdate)

      await(newStateFuture).allUsers ==> Seq(testUserA, testUserBUpdate.updatedEntity)
    }
  }
}
