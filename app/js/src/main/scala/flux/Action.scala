package flux

import models.accounting.{Transaction, TransactionGroup}
import scala.collection.immutable.Seq

sealed trait Action

object Action {

  case class AddTransactionGroup(transactionsWithoutId: Seq[Transaction]) extends Action
  case class RemoveTransactionGroup(transactionsGroupWithId: TransactionGroup) extends Action
}
