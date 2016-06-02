package common.testing

import play.api.test.FakeApplication
import play.api.test.Helpers._
import org.joda.time.DateTime

import common.Clock
import common.testing.TestObjects._
import models.{User, Users}
import models.accounting.config.{MoneyReservoir, Account, Category, Config}
import models.accounting.{Transaction, Transactions, TransactionGroups, TransactionGroup, BalanceCheck, BalanceChecks, Money}

object TestUtils {

  def persistTransaction(groupId: Long = -1,
                         flow: Money = Money(0),
                         date: DateTime = Clock.now,
                         timestamp: Long = -1,
                         account: Account = testAccount,
                         category: Category = testCategory,
                         reservoir: MoneyReservoir = testReservoir): Transaction = {
    val actualGroupId = if (groupId == -1) TransactionGroups.add(TransactionGroup()).id else groupId
    val actualDate = if (timestamp == -1) date else new DateTime(timestamp)
    Transactions.add(Transaction(
      transactionGroupId = actualGroupId,
      issuerId = 1,
      beneficiaryAccountCode = account.code,
      moneyReservoirCode = reservoir.code,
      categoryCode = category.code,
      description = "description",
      flow = flow,
      transactionDate = actualDate,
      consumedDate = actualDate
    ))
  }

  def persistBalanceCheck(balance: Money = Money(0),
                          date: DateTime = Clock.now,
                          timestamp: Long = -1,
                          reservoir: MoneyReservoir = testReservoir): BalanceCheck = {
    val actualDate = if (timestamp == -1) date else new DateTime(timestamp)
    BalanceChecks.add(BalanceCheck(
      issuerId = 2,
      moneyReservoirCode = reservoir.code,
      balance = balance,
      checkDate = actualDate
    ))
  }
}
