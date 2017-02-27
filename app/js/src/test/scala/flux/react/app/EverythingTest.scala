package flux.react.app

import common.I18n
import common.Formatting._
import common.time.Clock
import flux.react.app.Everything.NumEntriesStrategy
import flux.react.uielements
import flux.stores.LastNEntriesStoreFactory.{LastNEntriesState, N}
import flux.stores.{EntriesStore, LastNEntriesStoreFactory}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.EntityAccess
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq
import java.time.Month.JANUARY
import java.lang.Math.abs

import common.testing.{FakeClock, FakeRemoteDatabaseProxy, ReactTestWrapper, TestModule}
import common.time.{LocalDateTime, LocalDateTimes}
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{EntityModification, EntityType}
import utest._
import common.testing.TestObjects._
import flux.stores.LastNEntriesStoreFactory.LastNEntriesState
import flux.stores.entries.GeneralEntry
import models.User
import models.access.RemoteDatabaseProxy
import flux.stores.LastNEntriesStoreFactory.{LastNEntriesState, N}
import flux.stores.entries.GeneralEntry
import japgolly.scalajs.react.test.ReactTestUtils

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.util.Random
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
