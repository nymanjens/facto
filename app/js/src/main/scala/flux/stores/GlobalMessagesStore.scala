package flux.stores

import java.time.Instant

import common.LoggingUtils.logExceptions
import common.time.Clock
import common.time.JavaTimeImplicits._
import common.{I18n, Unique}
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

  private var _state: Option[Unique[Message]] = None

  private var stateUpdateListeners: Seq[GlobalMessagesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  def state: Option[Message] = _state.map(_.get)

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
          // Note: The delay is large because we don't want everything on the page to suddenly move up one row
          // while it is being used. This is expected to trigger when a user has left the page open while doing
          // something else.
          val uniqueStateWhenCreatedMessage = _state
          js.timers.setTimeout(2.minutes)(logExceptions {
            if (_state == uniqueStateWhenCreatedMessage) {
              // state has remained unchanged since start of timer
              setState(None)
            }
          })
        case None =>
      }

    case Action.SetPageLoadingState( /* isLoading = */ false) =>
      if (state.isDefined && state.get.age > java.time.Duration.ofSeconds(3)) {
        setState(None)
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
    _state = state.map(Unique.apply)
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
  case class Message private (string: String, isWorking: Boolean, private val createTime: Instant) {
    private[GlobalMessagesStore] def age(implicit clock: Clock): java.time.Duration =
      java.time.Duration.between(createTime, clock.nowInstant)
  }

  private[GlobalMessagesStore] object Message {
    def apply(string: String, isWorking: Boolean)(implicit clock: Clock): Message =
      Message(string = string, isWorking = isWorking, createTime = clock.nowInstant)
  }

  trait Listener {
    def onStateUpdate(): Unit
  }
}
