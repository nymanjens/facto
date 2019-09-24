package app.flux.stores.entries

import app.common.money.DatedMoney
import app.common.money.MoneyWithGeneralCurrency
import app.models.accounting.Transaction
import app.models.accounting._
import app.models.accounting.config.Config

import scala.collection.immutable.Seq

sealed trait CashFlowEntry {
  def balance(implicit accountingConfig: Config): DatedMoney
}

object CashFlowEntry {

  case class RegularEntry(
      override val transactions: Seq[Transaction],
      private val nonDatedBalance: MoneyWithGeneralCurrency,
      balanceVerified: Boolean,
  ) extends GroupedTransactions(transactions)
      with CashFlowEntry {

    override def balance(implicit accountingConfig: Config) = {
      val latestDate = transactions.map(_.transactionDate).max
      nonDatedBalance.withDate(latestDate)
    }
  }

  case class BalanceCorrection(balanceCheck: BalanceCheck, expectedAmount: MoneyWithGeneralCurrency)
      extends CashFlowEntry {
    override def balance(implicit accountingConfig: Config) = balanceCheck.balance

    def balanceIncrease(implicit accountingConfig: Config): MoneyWithGeneralCurrency =
      balanceCheck.balance - expectedAmount
  }
}
