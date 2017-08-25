package flux.action

import models.accounting.{BalanceCheck, Transaction, TransactionGroup}

import scala.collection.immutable.Seq

sealed trait Action

object Action {

  // **************** Transaction[Group]-related actions **************** //
  case class AddTransactionGroup(transactionsWithoutIdProvider: TransactionGroup => Seq[Transaction])
      extends Action
  case class UpdateTransactionGroup(transactionGroupWithId: TransactionGroup,
                                    transactionsWithoutId: Seq[Transaction])
      extends Action
  case class RemoveTransactionGroup(transactionGroupWithId: TransactionGroup) extends Action

  // **************** BalanceCheck-related actions **************** //
  case class AddBalanceCheck(balanceCheckWithoutId: BalanceCheck) extends Action
  case class UpdateBalanceCheck(existingBalanceCheck: BalanceCheck, newBalanceCheckWithoutId: BalanceCheck) extends Action
  case class RemoveBalanceCheck(existingBalanceCheck: BalanceCheck) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after they processed the contained action. */
  case class Done private[action] (action: Action) extends Action
}
