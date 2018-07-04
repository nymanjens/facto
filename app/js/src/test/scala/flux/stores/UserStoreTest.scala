package flux.stores

import java.time.Month.JANUARY

import api.ScalaJsApi.UserPrototype
import common.testing.TestObjects._
import common.testing.{Awaiter, FakeJsEntityAccess}
import common.time.LocalDateTime
import common.time.LocalDateTimes.createDateTime
import flux.action.Action
import flux.stores.BalanceCheckStoreTest.{assertAddBalanceCheck, testBalanceCheckWithoutId}
import flux.stores.entries.GeneralEntry.toGeneralEntrySeq
import models.accounting._
import models.modification.EntityModification
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

      await(Awaiter.expectEventuallyNonEmpty(store.state.get.allUsers))
      store.state.get.allUsers ==> Seq(testUserA)
    }

    "store calls listeners" - async {
      var onStateUpdateCount = 0
      store.register(() => {
        onStateUpdateCount += 1
      })

      await(Awaiter.expectEventuallyEqual(onStateUpdateCount, 1))

      entityAccess.addRemotelyAddedEntities(testUserA)

      await(Awaiter.expectEventuallyEqual(onStateUpdateCount, 2))

      entityAccess.addRemotelyAddedEntities(testUserA) // Duplicate
      entityAccess.addRemotelyAddedEntities(testTransactionWithIdB) // Irrelevant

      await(Awaiter.expectConsistentlyEqual(onStateUpdateCount, 2))
    }
  }
}
