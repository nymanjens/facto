// TODO: Re-enable this test (problem seems to be that ReactTestWrapper.children() doesn't work)

//package app.flux.react.app.transactionviews
//
//import app.common.testing.ReactTestWrapper
//import app.common.testing.TestModule
//import app.common.testing.TestObjects._
//import app.flux.stores.entries.factories.AllEntriesStoreFactory
//import app.models.accounting._
//import hydro.common.testing.Awaiter
//import hydro.common.testing.FakeRouterContext
//import japgolly.scalajs.react.vdom._
//import utest._
//
//import scala.async.Async.async
//import scala.async.Async.await
//import scala.collection.immutable.Seq
//import scala.concurrent.Future
//import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
//
//object EverythingTest extends TestSuite {
//
//  override def tests = TestSuite {
//    val testModule = new ThisTestModule()
//    implicit val entityAccess = testModule.fakeEntityAccess
//    implicit val clock = testModule.fakeClock
//    implicit val dispatcher = testModule.fakeDispatcher
//    val router = new FakeRouterContext()
//
//    val everything = testModule.everything
//
//    "empty" - async {
//      val tester = new ComponentTester(everything(router))
//      await(tester.waitNoLongerLoading)
//
//      tester.expandButton.isPresent ==> false
//      tester.countDataRows ==> 0
//    }
//
//    "nonempty" - async {
//      entityAccess.addRemotelyAddedEntities(uniqueTransactions(4))
//      entityAccess.addRemotelyAddedEntities(testUser)
//
//      val tester = new ComponentTester(everything(router))
//      await(tester.waitNoLongerLoading)
//
//      tester.expandButton.isPresent ==> false
//      tester.countDataRows ==> 4
//    }
//
//    "with expand button" - async {
//      entityAccess.addRemotelyAddedEntities(uniqueTransactions(435))
//      entityAccess.addRemotelyAddedEntities(testUser)
//
//      val tester = new ComponentTester(everything(router))
//      await(tester.waitNoLongerLoading)
//
//      tester.expandButton.isPresent ==> true
//      tester.countDataRows ==> 400
//
//      tester.expandButton.press()
//      await(tester.waitNoLongerLoading)
//
//      tester.expandButton.isPresent ==> false
//      tester.countDataRows ==> 435
//    }
//  }
//
//  private var uniqueTransactionId = 1010
//  private def uniqueTransaction(): Transaction = {
//    uniqueTransactionId += 1
//    testTransactionWithIdA.copy(
//      idOption = Some(uniqueTransactionId),
//      transactionGroupId = uniqueTransactionId)
//  }
//  private def uniqueTransactions(num: Int): Seq[Transaction] = {
//    for (_ <- 1 to num) yield uniqueTransaction()
//  }
//
//  private final class ComponentTester(unrenderedComponent: VdomElement) {
//    val component = ReactTestWrapper.renderComponent(unrenderedComponent)
//
//    def expandButton = new ExpandButton
//
//    def countDataRows = component.children(clazz = "data-row").length
//
//    def isLoading = component.children(clazz = "fa-spin").nonEmpty
//
//    def waitNoLongerLoading: Future[Unit] =
//      Awaiter.expectEventually.equal(isLoading, true)
//
//    final class ExpandButton {
//
//      def isPresent: Boolean = maybeComponent.isDefined
//
//      def press(): Unit = {
//        maybeComponent.get.click()
//      }
//
//      private def maybeComponent: Option[ReactTestWrapper] = {
//        component.maybeChild(clazz = "expand-num-entries", tpe = "button")
//      }
//    }
//  }
//
//  private final class ThisTestModule extends TestModule {
//
//    implicit val factory: AllEntriesStoreFactory = new AllEntriesStoreFactory
//    val everything: Everything = new Everything
//  }
//}
