package flux.stores.entries

import models.accounting.Transaction
import models.accounting.money.ReferenceMoney

import scala.collection.immutable.Seq

/**
  * @param debt The debt of the first account to the second (may be negative).
  */
case class LiquidationEntry(override val transactions: Seq[Transaction], debt: ReferenceMoney)
    extends GroupedTransactions(transactions)
