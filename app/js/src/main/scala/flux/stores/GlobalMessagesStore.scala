package flux.stores

import common.I18n
import common.time.Clock
import flux.action.Action.{AddTransactionGroup, RemoveTransactionGroup, UpdateTransactionGroup}
import flux.action.{Action, Dispatcher}
import flux.stores.GlobalMessagesStore.Message
import models.EntityAccess
import models.accounting._

import scala.collection.immutable.Seq

final class GlobalMessagesStore(implicit i18n: I18n,
                                clock: Clock,
                                entityAccess: EntityAccess,
                                dispatcher: Dispatcher) {
  dispatcher.registerPartial(dispatcherListener)


  private var _state: Option[Message] = None
  private var stateUpdateListeners: Seq[GlobalMessagesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: Option[Message] = _state

  final def register(listener: GlobalMessagesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  final def deregister(listener: GlobalMessagesStore.Listener): Unit = {
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
        // TODO: Set timer
        case None =>
      }
  }

  private def getCompletionMessage: PartialFunction[Action, String] = {
    case AddTransactionGroup(transactionsProvider) if numTransactions(transactionsProvider) == 1 =>
      i18n("facto.successfully-created-1-transaction")
    case AddTransactionGroup(transactionsProvider) =>
      i18n("facto.successfully-created-transactions", numTransactions(transactionsProvider))

    case UpdateTransactionGroup(group, transactions) if transactions.size == 1 =>
      i18n("facto.successfully-edited-1-transaction")
    case UpdateTransactionGroup(group, transactions) =>
      i18n("facto.successfully-edited-transactions", transactions.size)

    case RemoveTransactionGroup(group) if group.transactions.size == 1 =>
      i18n("facto.successfully-deleted-1-transaction")
    case RemoveTransactionGroup(group) =>
      i18n("facto.successfully-deleted-transactions", group.transactions.size)
  }

  private def numTransactions(transactionsProvider: TransactionGroup => Seq[Transaction]): Int = {
    transactionsProvider(TransactionGroup(createdDate = clock.now).withId(1)).size
  }

  // **************** Private state helper methods ****************//
  private def setState(state: Option[Message]): Unit = {
    _state = state
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
  case class Message(string: String,
                     isWorking: Boolean)

  trait Listener {
    def onStateUpdate(): Unit
  }
}
