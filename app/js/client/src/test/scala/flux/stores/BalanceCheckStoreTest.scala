package flux.stores

import common.testing.TestObjects._
import flux.action.Actions
import hydro.flux.action.StandardActions
import models.accounting._
import models.modification.EntityModification
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

object BalanceCheckStoreTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new common.testing.TestModule

    implicit val fakeDatabase = testModule.fakeEntityAccess
    implicit val fakeDispatcher = testModule.fakeDispatcher

    val balanceCheckStore = new BalanceCheckStore()

    "Listens to Actions.AddBalanceCheck" - async {
      await(fakeDispatcher.dispatch(Actions.AddBalanceCheck(testBalanceCheckWithoutId)))

      val Seq(addBc) = fakeDatabase.allModifications
      assertAddBalanceCheck(addBc, testBalanceCheckWithoutId)
    }

    "Listens to Actions.UpdateBalanceCheck" - async {
      fakeDatabase.addRemotelyAddedEntities(testBalanceCheckWithId)
      val initialModifications = fakeDatabase.allModifications
      val newBalanceCheck = testBalanceCheckWithId.copy(balanceInCents = 39877, idOption = None)

      await(fakeDispatcher.dispatch(Actions.UpdateBalanceCheck(testBalanceCheckWithId, newBalanceCheck)))

      fakeDatabase.allModifications.size - initialModifications.size ==> 2
      val Seq(removeBc, addBc) = fakeDatabase.allModifications takeRight 2
      removeBc ==> EntityModification.Remove[BalanceCheck](testBalanceCheckWithId.id)
      assertAddBalanceCheck(addBc, newBalanceCheck)
    }

    "Listens to Actions.RemoveBalanceCheck" - async {
      fakeDatabase.addRemotelyAddedEntities(testBalanceCheckWithId)
      val initialModifications = fakeDatabase.allModifications

      await(fakeDispatcher.dispatch(Actions.RemoveBalanceCheck(testBalanceCheckWithId)))

      fakeDatabase.allModifications.size - initialModifications.size ==> 1
      (fakeDatabase.allModifications takeRight 1) ==> Seq(
        EntityModification.Remove[BalanceCheck](testBalanceCheckWithId.id))
    }

    "Registers callback" - {
      fakeDispatcher.callbacks.size ==> 1
    }
  }

  private def testBalanceCheckWithoutId: BalanceCheck = testBalanceCheckWithId.copy(idOption = None)

  def assertAddBalanceCheck(modification: EntityModification, balanceCheckWithoutId: BalanceCheck) = {
    modification match {
      case EntityModification.Add(bc: BalanceCheck) =>
        bc ==> balanceCheckWithoutId.withId(bc.id)
      case _ => throw new java.lang.AssertionError(s"Not a balance check: $modification")
    }
  }
}
