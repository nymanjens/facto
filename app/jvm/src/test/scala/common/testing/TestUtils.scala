package common.testing

import java.time.{Instant, ZoneId}

import common.testing.TestObjects._
import common.time.LocalDateTime
import models.accounting.config.{Account, Category, MoneyReservoir}
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.modification.{EntityModification, EntityType}
import models.user.User
import models.{Entity, JvmEntityAccess}

import scala.collection.immutable.Seq

object TestUtils {

  def persistTransaction(groupId: Long = -1,
                         flowInCents: Long = 0,
                         date: LocalDateTime = FakeClock.defaultTime,
                         timestamp: Long = -1,
                         account: Account = testAccount,
                         category: Category = testCategory,
                         reservoir: MoneyReservoir = testReservoir,
                         description: String = "description",
                         detailDescription: String = "detailDescription",
                         tags: Seq[String] = Seq())(implicit entityAccess: JvmEntityAccess): Transaction = {
    val actualGroupId = if (groupId == -1) {
      persist(TransactionGroup(createdDate = FakeClock.defaultTime)).id
    } else {
      groupId
    }
    val actualDate = if (timestamp == -1) date else localDateTimeOfEpochMilli(timestamp)
    persist(
      Transaction(
        transactionGroupId = actualGroupId,
        issuerId = 1,
        beneficiaryAccountCode = account.code,
        moneyReservoirCode = reservoir.code,
        categoryCode = category.code,
        description = description,
        detailDescription = detailDescription,
        flowInCents = flowInCents,
        tags = tags,
        createdDate = actualDate,
        transactionDate = actualDate,
        consumedDate = actualDate
      ))
  }

  def persistBalanceCheck(
      balanceInCents: Long = 0,
      date: LocalDateTime = FakeClock.defaultTime,
      timestamp: Long = -1,
      reservoir: MoneyReservoir = testReservoir)(implicit entityAccess: JvmEntityAccess): BalanceCheck = {
    val actualDate = if (timestamp == -1) date else localDateTimeOfEpochMilli(timestamp)
    persist(
      BalanceCheck(
        issuerId = 2,
        moneyReservoirCode = reservoir.code,
        balanceInCents = balanceInCents,
        createdDate = actualDate,
        checkDate = actualDate
      ))
  }

  def persist[E <: Entity: EntityType](entity: E)(implicit entityAccess: JvmEntityAccess): E = {
    implicit val user = User(
      idOption = Some(9213982174887321L),
      loginName = "robot",
      passwordHash = "Some hash",
      name = "Robot",
      databaseEncryptionKey = "",
      expandCashFlowTablesByDefault = true)
    val addition =
      if (entity.idOption.isDefined) EntityModification.Add(entity)
      else EntityModification.createAddWithRandomId(entity)
    entityAccess.persistEntityModifications(addition)
    addition.entity
  }

  def localDateTimeOfEpochMilli(milli: Long): LocalDateTime = {
    val instant = Instant.ofEpochMilli(milli).atZone(ZoneId.of("Europe/Paris"))
    LocalDateTime.of(
      instant.toLocalDate,
      instant.toLocalTime
    )
  }
}
