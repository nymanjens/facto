package flux.stores.entries

import models.accounting.money.{DatedMoney, MoneyWithGeneralCurrency}
import models.accounting.{Transaction, _}

import scala.collection.immutable.Seq

sealed trait CashFlowEntry

object CashFlowEntry {

  case class RegularEntry(override val transactions: Seq[Transaction],
                          private val nonDatedBalance: MoneyWithGeneralCurrency,
                          balanceVerified: Boolean)
      extends GroupedTransactions(transactions)
      with CashFlowEntry {

    def balance: DatedMoney = {
      val latestDate = transactions.map(_.transactionDate).max
      nonDatedBalance.withDate(latestDate)
    }
  }

  case class BalanceCorrection(balanceCheck: BalanceCheck) extends CashFlowEntry
}
