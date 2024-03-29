package app.flux.action

import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.accounting.config.Category
import hydro.flux.action.Action

import scala.collection.immutable.Seq

object AppActions {

  // **************** Transaction[Group]-related actions **************** //
  case class AddTransactionGroup(transactionsWithoutIdProvider: TransactionGroup => Seq[Transaction])
      extends Action
  case class UpdateTransactionGroup(
      transactionGroupWithId: TransactionGroup,
      transactionsWithoutId: Seq[Transaction],
  ) extends Action
  case class RemoveTransactionGroup(transactionGroupWithId: TransactionGroup) extends Action

  // **************** BalanceCheck-related actions **************** //
  case class AddBalanceCheck(balanceCheckWithoutId: BalanceCheck) extends Action
  case class UpdateBalanceCheck(existingBalanceCheck: BalanceCheck, newBalanceCheckWithoutId: BalanceCheck)
      extends Action
  case class RemoveBalanceCheck(existingBalanceCheck: BalanceCheck) extends Action

  // **************** Refactor actions **************** //
  sealed trait RefactorAction extends Action {
    def transactions: Seq[Transaction]
    def updateToApply(transaction: Transaction): Transaction

    lazy val affectedTransactions: Seq[Transaction] = {
      transactions.filterNot(t => updateToApply(t) == t)
    }
  }
  case class EditAllChangeCategory(override val transactions: Seq[Transaction], newCategory: Category)
      extends RefactorAction {
    override def updateToApply(transaction: Transaction): Transaction = {
      transaction.copy(categoryCode = newCategory.code)
    }
  }
  case class EditAllAddTag(override val transactions: Seq[Transaction], newTag: String)
      extends RefactorAction {
    override def updateToApply(transaction: Transaction): Transaction = {
      if (transaction.tags contains newTag) transaction
      else transaction.copy(tags = transaction.tags :+ newTag)
    }
  }
}
