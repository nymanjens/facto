package common.testing

import play.api.test.FakeApplication
import play.api.test.Helpers._
import org.joda.time.DateTime
import common.Clock
import common.testing.TestObjects._
import models.{User, Users}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.Currency.Gbp
import models.accounting.money.{ExchangeRateMeasurement, ExchangeRateMeasurements, Money}
import models.accounting.{BalanceCheck, Transaction, TransactionGroup, TransactionGroups, Transactions}

object TestUtils {

  def persistTransaction(groupId: Long = -1,
                         flowInCents: Long = 0,
                         date: DateTime = Clock.now,
                         timestamp: Long = -1,
                         account: Account = testAccount,
                         category: Category = testCategory,
                         reservoir: MoneyReservoir = testReservoir,
                         description: String = "description",
                         detailDescription: String = "detailDescription",
                         tagsString: String = ""): Transaction = {
    val actualGroupId = if (groupId == -1) TransactionGroups.add(TransactionGroup()).id else groupId
    val actualDate = if (timestamp == -1) date else new DateTime(timestamp)
    Transactions.add(Transaction(
      transactionGroupId = actualGroupId,
      issuerId = 1,
      beneficiaryAccountCode = account.code,
      moneyReservoirCode = reservoir.code,
      categoryCode = category.code,
      description = description,
      detailDescription = detailDescription,
      flowInCents = flowInCents,
      tagsString = tagsString,
      transactionDate = actualDate,
      consumedDate = actualDate
    ))
  }

  def persistBalanceCheck(balanceInCents: Long = 0,
                          date: DateTime = Clock.now,
                          timestamp: Long = -1,
                          reservoir: MoneyReservoir = testReservoir): BalanceCheck = {
    val actualDate = if (timestamp == -1) date else new DateTime(timestamp)
    balanceCheckManager.add(BalanceCheck(
      issuerId = 2,
      moneyReservoirCode = reservoir.code,
      balanceInCents = balanceInCents,
      checkDate = actualDate
    ))
  }

  def persistGbpMeasurement(millisSinceEpoch: Long, ratio: Double): Unit = {
    ExchangeRateMeasurements.add(ExchangeRateMeasurement(
      date = new DateTime(millisSinceEpoch),
      foreignCurrencyCode = Gbp.code,
      ratioReferenceToForeignCurrency = ratio))
  }
}
