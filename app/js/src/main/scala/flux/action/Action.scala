package flux.action

import api.ScalaJsApi.UserPrototype
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}

import scala.collection.immutable.Seq

sealed trait Action

object Action {

  // **************** User-related actions **************** //
  case class UpsertUser(userPrototype: UserPrototype) extends Action

  // **************** Transaction[Group]-related actions **************** //
  case class AddTransactionGroup(transactionsWithoutIdProvider: TransactionGroup => Seq[Transaction])
      extends Action
  case class UpdateTransactionGroup(transactionGroupWithId: TransactionGroup,
                                    transactionsWithoutId: Seq[Transaction])
      extends Action
  case class RemoveTransactionGroup(transactionGroupWithId: TransactionGroup) extends Action

  // **************** BalanceCheck-related actions **************** //
  case class AddBalanceCheck(balanceCheckWithoutId: BalanceCheck) extends Action
  case class UpdateBalanceCheck(existingBalanceCheck: BalanceCheck, newBalanceCheckWithoutId: BalanceCheck)
      extends Action
  case class RemoveBalanceCheck(existingBalanceCheck: BalanceCheck) extends Action

  // **************** Other actions **************** //
  case class SetPageLoadingState(isLoading: Boolean) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after they processed the contained action. */
  case class Done private[action] (action: Action) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after processing an action failed. */
  case class Failed private[action] (action: Action) extends Action
}
