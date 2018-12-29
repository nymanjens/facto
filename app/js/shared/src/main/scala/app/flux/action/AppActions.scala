package app.flux.action

import hydro.flux.action.Action
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup

import scala.collection.immutable.Seq

object AppActions {

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
}
