package app.flux.stores.entries

import models.accounting.Transaction

import scala.collection.immutable.Seq

case class GeneralEntry(override val transactions: Seq[Transaction]) extends GroupedTransactions(transactions)

object GeneralEntry {
  def combineConsecutiveOfSameGroup(entries: Seq[GeneralEntry]): Seq[GeneralEntry] = {
    GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
      /* combine */
      (first, last) =>
        GeneralEntry(first.transactions ++ last.transactions)
    }
  }

  def toGeneralEntrySeq(transactionSeqs: Seq[Transaction]*): Seq[GeneralEntry] =
    transactionSeqs.map(GeneralEntry.apply).toVector
}
