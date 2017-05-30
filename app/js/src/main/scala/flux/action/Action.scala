package flux.action

import models.accounting.{Transaction, TransactionGroup}

import scala.collection.immutable.Seq

sealed trait Action

object Action {

  case class AddTransactionGroup(transactionsWithoutIdProvider: TransactionGroup => Seq[Transaction]) extends Action
  case class UpdateTransactionGroup(transactionGroupWithId: TransactionGroup, transactionsWithoutId: Seq[Transaction]) extends Action
  case class RemoveTransactionGroup(transactionGroupWithId: TransactionGroup) extends Action

  /** Special action that gets sent to the dispatcher's callbacks after they processed the contained action. */
  case class Done private[action](action: Action) extends Action
}
