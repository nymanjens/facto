package flux.react.app

import common.testing.TestObjects._
import common.testing.{ReactTestWrapper, TestModule}
import flux.stores.LastNEntriesStoreFactory
import japgolly.scalajs.react._
import models.accounting._
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object LastNEntriesStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new ThisTestModule()
    implicit val database = testModule.fakeRemoteDataProxy
    implicit val clock = testModule.fakeClock
    implicit val dispatcher = testModule.fakeDispatcher
    implicit val entityAccess = testModule.entityAccess

    val everything = testModule.everything

    "empty" - {
      val tester = new ComponentTester(everything())

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 0
    }

    "nonempty" - {
      database.addRemotelyAddedEntities(uniqueTransactions(4))
      database.addRemotelyAddedEntities(testUser)

      val tester = new ComponentTester(everything())

      tester.expandButton.isPresent ==> false
      tester.countDataRows ==> 4
    }

    "with expand button" - {
      database.addRemotelyAddedEntities(uniqueTransactions(35))
      database.addRemotelyAddedEntities(testUser)

      val tester = new ComponentTester(everything())

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

  private final class ComponentTester(unrenderedComponent: ReactElement) {
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

    implicit val factory: LastNEntriesStoreFactory = wire[LastNEntriesStoreFactory]
    val everything: Everything = wire[Everything]
  }
}
