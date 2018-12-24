package flux.stores

import common.testing.TestObjects._
import flux.action.Action
import models.accounting._
import models.modification.EntityModification
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

object TransactionAndGroupStoreTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new common.testing.TestModule

    implicit val fakeDatabase = testModule.fakeEntityAccess
    implicit val fakeClock = testModule.fakeClock
    implicit val fakeDispatcher = testModule.fakeDispatcher

    val transactionAndGroupStore = new TransactionAndGroupStore()

    "Listens to Action.AddTransactionGroup" - async {
      var groupId: Long = -1
      await(fakeDispatcher.dispatch(Action.AddTransactionGroup(transactionsWithoutIdProvider = group => {
        groupId = group.id
        Seq(testTransactionWithId.copy(idOption = None, transactionGroupId = group.id))
      })))

      assert(groupId > 0)

      fakeDatabase.allModifications.size ==> 2
      val Seq(addGroup, addTransaction) = fakeDatabase.allModifications

      addGroup ==> EntityModification.Add(
        TransactionGroup(idOption = Some(groupId), createdDate = fakeClock.now))

      addTransaction match {
        case EntityModification.Add(transaction: Transaction) =>
          transaction ==> testTransactionWithId
            .copy(idOption = Some(transaction.id), transactionGroupId = groupId)
        case _ => throw new java.lang.AssertionError(addTransaction)
      }
    }

    "Listens to Action.UpdateTransactionGroup" - async {
      fakeDatabase.addRemotelyAddedEntities(testTransactionGroupWithId)
      fakeDatabase.addRemotelyAddedEntities(testTransactionWithId)
      val initialModifications = fakeDatabase.allModifications
      await(
        fakeDispatcher.dispatch(
          Action.UpdateTransactionGroup(
            transactionGroupWithId = testTransactionGroupWithId,
            transactionsWithoutId = Seq(
              testTransactionWithIdB.copy(idOption = None)
            ))))

      fakeDatabase.allModifications.size - initialModifications.size ==> 2
      val Seq(removeTransaction, addTransaction) = fakeDatabase.allModifications takeRight 2

      removeTransaction ==> EntityModification.Remove[Transaction](testTransactionWithId.id)

      addTransaction match {
        case EntityModification.Add(transaction: Transaction) =>
          transaction ==> testTransactionWithIdB
            .copy(idOption = Some(transaction.id), transactionGroupId = testTransactionGroupWithId.id)
        case _ => throw new java.lang.AssertionError(addTransaction)
      }
    }

    "Listens to Action.RemoveTransactionGroup" - async {
      fakeDatabase.addRemotelyAddedEntities(testTransactionGroupWithId)
      fakeDatabase.addRemotelyAddedEntities(testTransactionWithId)
      val initialModifications = fakeDatabase.allModifications
      await(fakeDispatcher.dispatch(Action.RemoveTransactionGroup(testTransactionGroupWithId)))

      fakeDatabase.allModifications.size - initialModifications.size ==> 2
      val Seq(removeTransaction, removeGroup) = fakeDatabase.allModifications takeRight 2

      removeTransaction ==> EntityModification.Remove[Transaction](testTransactionWithId.id)
      removeGroup ==> EntityModification.Remove[TransactionGroup](testTransactionGroupWithId.id)
    }

    "Registers callback" - {
      fakeDispatcher.callbacks.size ==> 1
    }
  }
}
