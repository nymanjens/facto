package flux.action

import models.accounting.{Transaction, TransactionGroup}

import scala.collection.immutable.Seq

sealed trait Action

object Action {

  case class AddTransactionGroup(transactionsWithoutIdProvider: TransactionGroup => Seq[Transaction]) extends Action
  case class UpdateTransactionGroup(transactionGroupWithId: TransactionGroup, transactionsWithoutId: Seq[Transaction]) extends Action
  case class RemoveTransactionGroup(transactionGroupWithId: TransactionGroup) extends Action
}
