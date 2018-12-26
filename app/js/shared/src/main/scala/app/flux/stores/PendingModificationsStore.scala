package app.flux.stores

import app.flux.stores.PendingModificationsStore.State
import hydro.flux.stores.StateStore
import models.access.JsEntityAccess
import models.accounting.Transaction
import models.modification.EntityModification
import models.modification.EntityType

import scala.collection.immutable.Seq
import scala.collection.mutable

final class PendingModificationsStore(implicit jsEntityAccess: JsEntityAccess) extends StateStore[State] {
  jsEntityAccess.registerListener(JsEntityAccessListener)

  private var _state: State = State(numberOfModifications = 0)

  // **************** Public API ****************//
  override def state: State = _state

  // **************** Private state helper methods ****************//
  private def setState(state: State): Unit = {
    val originalState = _state
    _state = state
    if (_state != originalState) {
      invokeStateUpdateListeners()
    }
  }

  // **************** Private inner types ****************//
  object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit =
      onAnyChange()

    override def pendingModificationsPersistedLocally(): Unit = onAnyChange()

    private def onAnyChange(): Unit = {
      if (jsEntityAccess.pendingModifications.persistedLocally) {
        setState(
          State(
            numberOfModifications = getModificationsSize(jsEntityAccess.pendingModifications.modifications)))
      } else {
        setState(State(numberOfModifications = 0))
      }
    }

    private def getModificationsSize(modifications: Seq[EntityModification]): Int = {
      val affectedTransactionGroupIds = mutable.Set[Long]()
      var nonTransactionEditCount = 0

      for (modification <- modifications) modification.entityType match {
        case EntityType.TransactionType =>
          modification match {
            case EntityModification.Add(entity) =>
              affectedTransactionGroupIds += entity.asInstanceOf[Transaction].transactionGroupId
            case _ =>
          }
        case EntityType.TransactionGroupType        => affectedTransactionGroupIds += modification.entityId
        case EntityType.UserType                    => nonTransactionEditCount += 1
        case EntityType.BalanceCheckType            => nonTransactionEditCount += 1
        case EntityType.ExchangeRateMeasurementType => nonTransactionEditCount += 1
      }

      affectedTransactionGroupIds.size + nonTransactionEditCount
    }
  }
}
object PendingModificationsStore {

  /** numberOfModifications: Number of pending modifications that have been persisted locally. */
  case class State(numberOfModifications: Int)
}
