package models.access

import common.time.LocalDateTime
import models.Entity
import models.money.ExchangeRateMeasurement
import models.user.User

import scala.collection.immutable.Seq

object Fields {
  def id[E <: Entity]: ModelField[Long, E] = ModelField("id")

  object User {
    private type E = User

    val loginName: ModelField[String, E] = ModelField("loginName")
    val passwordHash: ModelField[String, E] = ModelField("passwordHash")
    val name: ModelField[String, E] = ModelField("name")
    val databaseEncryptionKey: ModelField[String, E] = ModelField("databaseEncryptionKey")
    val expandCashFlowTablesByDefault: ModelField[Boolean, E] = ModelField("expandCashFlowTablesByDefault")
  }

  object Transaction {
    private type E = models.accounting.Transaction

    val transactionGroupId: ModelField[Long, E] = ModelField("transactionGroupId")
    val issuerId: ModelField[Long, E] = ModelField("issuerId")
    val beneficiaryAccountCode: ModelField[String, E] = ModelField("beneficiaryAccountCode")
    val moneyReservoirCode: ModelField[String, E] = ModelField("moneyReservoirCode")
    val categoryCode: ModelField[String, E] = ModelField("categoryCode")
    val description: ModelField[String, E] = ModelField("description")
    val flowInCents: ModelField[Long, E] = ModelField("flowInCents")
    val detailDescription: ModelField[String, E] = ModelField("detailDescription")
    val tags: ModelField[Seq[String], E] = ModelField("tags")
    val createdDate: ModelField[LocalDateTime, E] = ModelField("createdDate")
    val transactionDate: ModelField[LocalDateTime, E] = ModelField("transactionDate")
    val consumedDate: ModelField[LocalDateTime, E] = ModelField("consumedDate")
  }

  object TransactionGroup {
    private type E = models.accounting.TransactionGroup

    val createdDate: ModelField[LocalDateTime, E] = ModelField("createdDate")
  }

  object BalanceCheck {
    private type E = models.accounting.BalanceCheck

    val issuerId: ModelField[Long, E] = ModelField("issuerId")
    val moneyReservoirCode: ModelField[String, E] = ModelField("moneyReservoirCode")
    val balanceInCents: ModelField[Long, E] = ModelField("balanceInCents")
    val createdDate: ModelField[LocalDateTime, E] = ModelField("createdDate")
    val checkDate: ModelField[LocalDateTime, E] = ModelField("checkDate")
  }

  object ExchangeRateMeasurement {
    private type E = ExchangeRateMeasurement

    val date: ModelField[LocalDateTime, E] = ModelField("date")
    val foreignCurrencyCode: ModelField[String, E] = ModelField("foreignCurrencyCode")
    val ratioReferenceToForeignCurrency: ModelField[Double, E] = ModelField(
      "ratioReferenceToForeignCurrency")
  }
}
