package flux.stores

import api.ScalaJsApi.UserPrototype
import common.testing.Awaiter
import common.testing.TestObjects._
import flux.action.Action
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

// Also tests `AsyncEntityDerivedStateStore`
object UserStoreTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new common.testing.TestModule
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val fakeDispatcher = testModule.fakeDispatcher
    implicit val fakeScalaJsApiClient = testModule.fakeScalaJsApiClient

    val store: UserStore = new UserStore()

    "Listens to Action.UpsertUser" - async {
      val userPrototype = UserPrototype.create(id = 19283L)

      await(fakeDispatcher.dispatch(Action.UpsertUser(userPrototype)))

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
      entityAccess.addRemotelyAddedEntities(testTransactionWithIdB) // Irrelevant

      await(Awaiter.expectConsistently.equal(onStateUpdateCount, 2))
    }
  }
}
