package flux.stores

import common.I18n
import common.LoggingUtils.logExceptions
import common.time.Clock
import flux.action.Action._
import flux.action.{Action, Dispatcher}
import flux.stores.GlobalMessagesStore.Message
import models.EntityAccess
import models.accounting._
import models.accounting.config.Config

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.scalajs.js

final class GlobalMessagesStore(implicit i18n: I18n,
                                clock: Clock,
                                entityAccess: EntityAccess,
                                accountingConfig: Config,
                                dispatcher: Dispatcher) {
  dispatcher.registerPartialSync(dispatcherListener)

  private var _state: Option[Message] = None

  /** Gets incremented whenever _state is updated. */
  private var stateChangedCounter: Long = 0
  private var stateUpdateListeners: Seq[GlobalMessagesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  def state: Option[Message] = _state

  def register(listener: GlobalMessagesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  def deregister(listener: GlobalMessagesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Private dispatcher methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case action if getCompletionMessage.isDefinedAt(action) =>
      setState(Some(Message(string = i18n("facto.sending-data-to-server"), isWorking = true)))

    case Action.Done(action) =>
      getCompletionMessage.lift.apply(action) match {
        case Some(message) =>
          setState(Some(Message(string = message, isWorking = false)))

          // Clear this message after some delay
          val counterWhenCreatedMessage = stateChangedCounter
          js.timers.setTimeout(2.minutes)(logExceptions {
            if (stateChangedCounter == counterWhenCreatedMessage) {
              // state has remained unchanged since start of timer
              setState(None)
            }
          })
        case None =>
      }
  }

  private def getCompletionMessage: PartialFunction[Action, String] = {
    // **************** Transaction[Group]-related actions **************** //
    case AddTransactionGroup(transactionsProvider) if numTransactions(transactionsProvider) == 1 =>
      i18n("facto.successfully-created-1-transaction")
    case AddTransactionGroup(transactionsProvider) =>
      i18n("facto.successfully-created-transactions", numTransactions(transactionsProvider))

    case UpdateTransactionGroup(group, transactions) if transactions.size == 1 =>
      i18n("facto.successfully-edited-1-transaction")
    case UpdateTransactionGroup(group, transactions) =>
      i18n("facto.successfully-edited-transactions", transactions.size)

    case RemoveTransactionGroup(group) =>
      i18n("facto.successfully-deleted-transactions")

    // **************** BalanceCheck-related actions **************** //
    case AddBalanceCheck(balanceCheck) =>
      i18n("facto.successfully-created-a-balance-check-for", balanceCheck.moneyReservoir.name)
    case UpdateBalanceCheck(existingBalanceCheck, newBalanceCheck) =>
      i18n("facto.successfully-edited-a-balance-check-for", newBalanceCheck.moneyReservoir.name)
    case RemoveBalanceCheck(existingBalanceCheck) =>
      i18n("facto.successfully-deleted-balance-check-for", existingBalanceCheck.moneyReservoir.name)
  }

  private def numTransactions(transactionsProvider: TransactionGroup => Seq[Transaction]): Int = {
    transactionsProvider(TransactionGroup(createdDate = clock.now).withId(1)).size
  }

  // **************** Private state helper methods ****************//
  private def setState(state: Option[Message]): Unit = {
    _state = state
    stateChangedCounter += 1
    invokeListeners()
  }

  private def invokeListeners(): Unit = {
    require(!isCallingListeners)
    isCallingListeners = true
    stateUpdateListeners.foreach(_.onStateUpdate())
    isCallingListeners = false
  }
}

object GlobalMessagesStore {
  case class Message(string: String, isWorking: Boolean)

  trait Listener {
    def onStateUpdate(): Unit
  }
}
