package flux

import java.lang

import common.testing.TestObjects._
import common.testing.{FakeScalaJsApiClient, ModificationsBuffer}
import flux.action.{Action, Dispatcher}
import jsfacades.LokiJs
import models.accounting.Transaction
import models.manager.EntityType.TransactionType
import models.manager.{Entity, EntityModification, EntityType}
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala2js.Converters._

object DispatcherTest extends TestSuite {

  override def tests = TestSuite {
    val dispatcher: Dispatcher.Impl = new Dispatcher.Impl()
    val testAction = Action.RemoveTransactionGroup(testTransactionGroupWithId)

    "dispatches actions to listeners, including Done action" - async {
      val dispatchedActions: mutable.Buffer[Action] = mutable.Buffer()
      dispatcher.registerAsync(action => {
        dispatchedActions += action
        Future.successful((): Unit)
      })

      await(dispatcher.dispatch(testAction))

      dispatchedActions ==> mutable.Buffer(testAction, Action.Done(testAction))
    }

    "does not allow dispatching during the sync part of a callback" - async {
      var dispatched = false
      dispatcher.registerAsync(action => {
        try {
          dispatcher.dispatch(testAction)
          throw new java.lang.AssertionError("expected IllegalArgumentException")
        } catch {
          case e: IllegalArgumentException => // expected
        }
        dispatched = true
        Future.successful((): Unit)
      })

      await(dispatcher.dispatch(testAction))

      dispatched ==> true
    }
  }
}
