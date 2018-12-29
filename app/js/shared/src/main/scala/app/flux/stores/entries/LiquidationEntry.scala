package app.flux.stores.entries

import app.common.money.ReferenceMoney
import app.models.accounting.Transaction

import scala.collection.immutable.Seq

/**
  * @param debt The debt of the first account to the second (may be negative).
  */
case class LiquidationEntry(override val transactions: Seq[Transaction], debt: ReferenceMoney)
    extends GroupedTransactions(transactions)
