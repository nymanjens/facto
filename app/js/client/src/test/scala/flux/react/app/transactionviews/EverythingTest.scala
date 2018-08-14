package flux.react.app.transactionviews

import common.testing.TestObjects._
import common.testing.{FakeRouterContext, ReactTestWrapper, TestModule}
import flux.stores.entries.factories.AllEntriesStoreFactory
import japgolly.scalajs.react.vdom._
import models.accounting._
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala2js.Converters._

object EverythingTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val entityAccess = testModule.fakeEntityAccess
    implicit val clock = testModule.fakeClock
    implicit val dispatcher = testModule.fakeDispatcher
    val router = new FakeRouterContext()

    val everything = testModule.everything

    "empty" - async {
      val tester = new ComponentTester(everything(router))
      await(tester.waitNoLongerLoading)

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 0
    }

    "nonempty" - async {
      entityAccess.addRemotelyAddedEntities(uniqueTransactions(4))
      entityAccess.addRemotelyAddedEntities(testUser)

      val tester = new ComponentTester(everything(router))
      await(tester.waitNoLongerLoading)

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 4
    }

    "with expand button" - async {
      entityAccess.addRemotelyAddedEntities(uniqueTransactions(435))
      entityAccess.addRemotelyAddedEntities(testUser)

      val tester = new ComponentTester(everything(router))
      await(tester.waitNoLongerLoading)

      tester.expandButton.isPresent ==> true
      tester.countDataRows ==> 400

      tester.expandButton.press()
      await(tester.waitNoLongerLoading)

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 435
    }
  }

  private var uniqueTransactionId = 1010
  private def uniqueTransaction(): Transaction = {
    uniqueTransactionId += 1
    testTransactionWithIdA.copy(
      idOption = Some(uniqueTransactionId),
      transactionGroupId = uniqueTransactionId)
  }
  private def uniqueTransactions(num: Int): Seq[Transaction] = {
    for (_ <- 1 to num) yield uniqueTransaction()
  }

  private final class ComponentTester(unrenderedComponent: VdomElement) {
    val component = ReactTestWrapper.renderComponent(unrenderedComponent)

    def expandButton = new ExpandButton

    def countDataRows = component.children(clazz = "data-row").length

    def isLoading = component.children(clazz = "fa-spin").nonEmpty

    def waitNoLongerLoading: Future[Unit] = {
      val promise = Promise[Unit]()

      def cyclicLogic(): Unit = {
        if (isLoading) {
          js.timers.setTimeout(20.milliseconds)(cyclicLogic())
        } else {
          promise.success((): Unit)
        }
      }

      js.timers.setTimeout(0)(cyclicLogic())
      promise.future
    }

    /** Excludes data rows **/
    //    def numberOfDataRows

    final class ExpandButton {

      def isPresent: Boolean = maybeComponent.isDefined

      def press(): Unit = {
        maybeComponent.get.click()
      }

      private def maybeComponent: Option[ReactTestWrapper] = {
        component.maybeChild(clazz = "expand-num-entries", tpe = "button")
      }
    }
  }

  private final class ThisTestModule extends TestModule {

    implicit val factory: AllEntriesStoreFactory = new AllEntriesStoreFactory
    val everything: Everything = new Everything
  }
}
