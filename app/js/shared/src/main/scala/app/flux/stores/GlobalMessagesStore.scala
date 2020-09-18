package app.flux.stores

import java.time.Instant

import app.flux.action.AppActions._
import app.flux.stores.GlobalMessagesStore.Message
import app.models.accounting._
import app.models.accounting.config.Config
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.Unique
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.flux.action.Action
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.action.StandardActions._
import hydro.flux.stores.StateStore
import hydro.models.access.EntityAccess
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class GlobalMessagesStore(implicit
    i18n: I18n,
    clock: Clock,
    entityAccess: EntityAccess,
    accountingConfig: Config,
    dispatcher: Dispatcher,
) extends StateStore[Option[Message]] {
  dispatcher.registerPartialSync(dispatcherListener)
  startCheckingLoggedInState()

  private var _state: Option[Unique[Message]] = None

  // **************** Public API ****************//
  override def state: Option[Message] = _state.map(_.get)

  // **************** Private dispatcher methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case action if getCompletionMessage.isDefinedAt(action) =>
      setState(Message(string = i18n("app.sending-data-to-server"), messageType = Message.Type.Working))

    case StandardActions.Done(action) =>
      getCompletionMessage.lift.apply(action) match {
        case Some(message) =>
          setState(Message(string = message, messageType = Message.Type.Success))
          clearMessageAfterDelay()
        case None =>
      }

    case StandardActions.Failed(action) =>
      getCompletionMessage.lift.apply(action) match {
        case Some(message) =>
          setState(
            Message(string = i18n("app.sending-data-to-server-failed"), messageType = Message.Type.Failure)
          )
          clearMessageAfterDelay()
        case None =>
      }

    case StandardActions.SetPageLoadingState( /* isLoading = */ false) =>
      if (state.isDefined && state.get.age > java.time.Duration.ofSeconds(1)) {
        setState(None)
      }
  }

  private def getCompletionMessage: PartialFunction[Action, String] = {
    // **************** User-related actions **************** //
    case UpsertUser(userPrototype)
        if userPrototype.id.isDefined && userPrototype.plainTextPassword.isDefined =>
      i18n("app.successfully-updated-password")
    case UpsertUser(userPrototype) if userPrototype.id.isEmpty =>
      i18n("app.successfully-added-user", userPrototype.loginName getOrElse "<Unknown name>")

    // **************** Transaction[Group]-related actions **************** //
    case AddTransactionGroup(transactionsProvider) if numTransactions(transactionsProvider) == 1 =>
      i18n("app.successfully-created-1-transaction")
    case AddTransactionGroup(transactionsProvider) =>
      i18n("app.successfully-created-transactions", numTransactions(transactionsProvider))

    case UpdateTransactionGroup(group, transactions) if transactions.size == 1 =>
      i18n("app.successfully-edited-1-transaction")
    case UpdateTransactionGroup(group, transactions) =>
      i18n("app.successfully-edited-transactions", transactions.size)

    case RemoveTransactionGroup(group) =>
      i18n("app.successfully-deleted-transactions")

    // **************** BalanceCheck-related actions **************** //
    case AddBalanceCheck(balanceCheck) =>
      i18n("app.successfully-created-a-balance-check-for", balanceCheck.moneyReservoir.name)
    case UpdateBalanceCheck(existingBalanceCheck, newBalanceCheck) =>
      i18n("app.successfully-edited-a-balance-check-for", newBalanceCheck.moneyReservoir.name)
    case RemoveBalanceCheck(existingBalanceCheck) =>
      i18n("app.successfully-deleted-balance-check-for", existingBalanceCheck.moneyReservoir.name)
  }

  /** Clear this message after some delay */
  private def clearMessageAfterDelay(): Unit = {
    val uniqueStateWhenCreatedMessage = _state
    js.timers.setTimeout(2.seconds)(logExceptions {
      if (_state == uniqueStateWhenCreatedMessage) {
        // state has remained unchanged since start of timer
        setState(None)
      }
    })
  }

  private def numTransactions(transactionsProvider: TransactionGroup => Seq[Transaction]): Int = {
    transactionsProvider(TransactionGroup(createdDate = clock.now).withId(1)).size
  }

  // **************** Private state helper methods ****************//
  private def setState(message: Message): Unit = {
    setState(Some(message))
  }
  private def setState(state: Option[Message]): Unit = {
    _state = state.map(Unique.apply)
    invokeStateUpdateListeners()
  }

  private def startCheckingLoggedInState(): Unit = {
    dom.window.setInterval(() => checkLoggedInState(), timeout = 60 * 1000.0)
  }

  private def checkLoggedInState(): Unit = async {
    val ajaxResponse = await(dom.ext.Ajax.get(url = "/loggedin/"))
    if (ajaxResponse.responseText == "false") {
      setState(Message(string = i18n("app.no-longer-logged-in"), messageType = Message.Type.Failure))
    }
  }
}

object GlobalMessagesStore {
  case class Message private (string: String, messageType: Message.Type, private val createTime: Instant) {
    private[GlobalMessagesStore] def age(implicit clock: Clock): java.time.Duration =
      java.time.Duration.between(createTime, clock.nowInstant)
  }

  object Message {
    def apply(string: String, messageType: Message.Type)(implicit clock: Clock): Message =
      Message(string = string, messageType = messageType, createTime = clock.nowInstant)

    sealed trait Type
    object Type {
      object Working extends Type
      object Success extends Type
      object Failure extends Type
    }
  }
}
