package flux.stores

import common.testing.TestObjects._
import flux.action.Action
import models.accounting._
import models.manager.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object BalanceCheckStoreTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new common.testing.TestModule

    implicit val fakeDatabase = testModule.fakeRemoteDatabaseProxy
    implicit val fakeDispatcher = testModule.fakeDispatcher

    val balanceCheckStore = new BalanceCheckStore()

    "Listens to Action.AddBalanceCheck" - {
      fakeDispatcher.dispatch(Action.AddBalanceCheck(testBalanceCheckWithoutId))

      val Seq(addBc) = fakeDatabase.allModifications
      assertAddBalanceCheck(addBc, testBalanceCheckWithoutId)
    }

    "Listens to Action.UpdateBalanceCheck" - {
      fakeDatabase.addRemotelyAddedEntities(testBalanceCheckWithId)
      val initialModifications = fakeDatabase.allModifications
      val newBalanceCheck = testBalanceCheckWithId.copy(balanceInCents = 39877)

      fakeDispatcher.dispatch(Action.UpdateBalanceCheck(newBalanceCheck))

      fakeDatabase.allModifications.size - initialModifications.size ==> 2
      val Seq(removeBc, addBc) = fakeDatabase.allModifications takeRight 2
      removeBc ==> EntityModification.Remove[BalanceCheck](testBalanceCheckWithId.id)
      assertAddBalanceCheck(addBc, newBalanceCheck)
    }

    "Listens to Action.RemoveBalanceCheck" - {
      fakeDatabase.addRemotelyAddedEntities(testBalanceCheckWithId)
      val initialModifications = fakeDatabase.allModifications

      fakeDispatcher.dispatch(Action.RemoveBalanceCheck(testBalanceCheckWithId))

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
