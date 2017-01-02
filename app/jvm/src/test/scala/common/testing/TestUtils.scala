package common.testing

import java.time.{Instant, ZoneId}

import common.testing.TestObjects._
import common.time.LocalDateTime
import models.{EntityAccess, SlickEntityAccess, User}
import models.accounting.config.{Account, Category, MoneyReservoir}
import models.accounting.money.Currency.Gbp
import models.accounting.money.ExchangeRateMeasurement
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}

object TestUtils {

  def getPersistedTestUser(implicit entityAccess: SlickEntityAccess): User = {
    val user = TestObjects.testUser
    if (entityAccess.userManager.findByLoginName(user.loginName).isEmpty) {
      entityAccess.userManager.addWithId(user)
    }
    user
  }

  def persistTransaction(groupId: Long = -1,
                         flowInCents: Long = 0,
                         date: LocalDateTime = FakeClock.defaultTime,
                         timestamp: Long = -1,
                         account: Account = testAccount,
                         category: Category = testCategory,
                         reservoir: MoneyReservoir = testReservoir,
                         description: String = "description",
                         detailDescription: String = "detailDescription",
                         tagsString: String = "")(implicit entityAccess: SlickEntityAccess): Transaction = {
    val actualGroupId = if (groupId == -1) {
      entityAccess.transactionGroupManager.add(TransactionGroup(createdDate = FakeClock.defaultTime)).id
    } else {
      groupId
    }
    val actualDate = if (timestamp == -1) date else localDateTimeOfEpochMilli(timestamp)
    entityAccess.transactionManager.add(Transaction(
      transactionGroupId = actualGroupId,
      issuerId = 1,
      beneficiaryAccountCode = account.code,
      moneyReservoirCode = reservoir.code,
      categoryCode = category.code,
      description = description,
      detailDescription = detailDescription,
      flowInCents = flowInCents,
      tagsString = tagsString,
      createdDate = actualDate,
      transactionDate = actualDate,
      consumedDate = actualDate
    ))
  }

  def persistBalanceCheck(balanceInCents: Long = 0,
                          date: LocalDateTime = FakeClock.defaultTime,
                          timestamp: Long = -1,
                          reservoir: MoneyReservoir = testReservoir)(implicit entityAccess: SlickEntityAccess): BalanceCheck = {
    val actualDate = if (timestamp == -1) date else localDateTimeOfEpochMilli(timestamp)
    entityAccess.balanceCheckManager.add(BalanceCheck(
      issuerId = 2,
      moneyReservoirCode = reservoir.code,
      balanceInCents = balanceInCents,
      createdDate = actualDate,
      checkDate = actualDate
    ))
  }

  def persistGbpMeasurement(millisSinceEpoch: Long, ratio: Double)(implicit entityAccess: SlickEntityAccess): Unit = {
    entityAccess.exchangeRateMeasurementManager.add(ExchangeRateMeasurement(
      date = localDateTimeOfEpochMilli(millisSinceEpoch),
      foreignCurrencyCode = Gbp.code,
      ratioReferenceToForeignCurrency = ratio))
  }

  def localDateTimeOfEpochMilli(milli: Long): LocalDateTime = {
    val instant = Instant.ofEpochMilli(milli).atZone(ZoneId.of("Europe/Paris"))
    LocalDateTime.of(
      instant.toLocalDate,
      instant.toLocalTime
    )
  }
}
