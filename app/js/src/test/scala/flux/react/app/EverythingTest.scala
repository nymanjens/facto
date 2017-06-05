package flux.react.app

import common.testing.TestObjects._
import common.testing.{FakeRouterCtl, ReactTestWrapper, TestModule}
import flux.stores.AllEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import models.accounting._
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object EverythingTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy
    implicit val clock = testModule.fakeClock
    implicit val dispatcher = testModule.fakeDispatcher
    implicit val entityAccess = testModule.entityAccess
    val router = new FakeRouterCtl()

    val everything = testModule.everything

    "empty" - {
      val tester = new ComponentTester(everything(router))

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 0
    }

    "nonempty" - {
      database.addRemotelyAddedEntities(uniqueTransactions(4))
      database.addRemotelyAddedEntities(testUser)

      val tester = new ComponentTester(everything(router))

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 4
    }

    "with expand button" - {
      database.addRemotelyAddedEntities(uniqueTransactions(35))
      database.addRemotelyAddedEntities(testUser)

      val tester = new ComponentTester(everything(router))

      tester.expandButton.isPresent ==> true
      tester.countDataRows ==> 5

      tester.expandButton.press()

      tester.expandButton.isPresent ==> true
      tester.countDataRows ==> 30

      tester.expandButton.press()

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 35
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

    /** Excludes data rows **/
    //    def numberOfDataRows

    final class ExpandButton {

      def isPresent: Boolean = maybeComponent.isDefined

      def press(): Unit = {
        maybeComponent.get.click
      }

      private def maybeComponent: Option[ReactTestWrapper] = {
        component.maybeChild(clazz = "expand-num-entries", tpe = "button")
      }
    }
  }

  private final class ThisTestModule extends TestModule {

    import com.softwaremill.macwire._

    implicit val factory: AllEntriesStoreFactory = wire[AllEntriesStoreFactory]
    val everything: Everything = wire[Everything]
  }
}
