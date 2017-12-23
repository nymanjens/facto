package models.access

import common.time.LocalDateTime
import models.Entity
import models.money.ExchangeRateMeasurement
import models.user.User

import scala.collection.immutable.Seq

object Fields {
  def id[E <: Entity]: ModelField[Long, E] = new ModelField("id", _.idOption getOrElse -1)

  object User {
    private type E = User

    val loginName: ModelField[String, E] = new ModelField("loginName", _.loginName)
    val passwordHash: ModelField[String, E] = new ModelField("passwordHash", _.passwordHash)
    val name: ModelField[String, E] = new ModelField("name", _.name)
    val databaseEncryptionKey: ModelField[String, E] =
      new ModelField("databaseEncryptionKey", _.databaseEncryptionKey)
    val expandCashFlowTablesByDefault: ModelField[Boolean, E] =
      new ModelField("expandCashFlowTablesByDefault", _.expandCashFlowTablesByDefault)
  }

  object Transaction {
    private type E = models.accounting.Transaction

    val transactionGroupId: ModelField[Long, E] = new ModelField("transactionGroupId", _.transactionGroupId)
    val issuerId: ModelField[Long, E] = new ModelField("issuerId", _.issuerId)
    val beneficiaryAccountCode: ModelField[String, E] =
      new ModelField("beneficiaryAccountCode", _.beneficiaryAccountCode)
    val moneyReservoirCode: ModelField[String, E] =
      new ModelField("moneyReservoirCode", _.moneyReservoirCode)
    val categoryCode: ModelField[String, E] = new ModelField("categoryCode", _.categoryCode)
    val description: ModelField[String, E] = new ModelField("description", _.description)
    val flowInCents: ModelField[Long, E] = new ModelField("flowInCents", _.flowInCents)
    val detailDescription: ModelField[String, E] = new ModelField("detailDescription", _.detailDescription)
    val tags: ModelField[Seq[String], E] = new ModelField("tags", _.tags)
    val createdDate: ModelField[LocalDateTime, E] = new ModelField("createdDate", _.createdDate)
    val transactionDate: ModelField[LocalDateTime, E] = new ModelField("transactionDate", _.transactionDate)
    val consumedDate: ModelField[LocalDateTime, E] = new ModelField("consumedDate", _.consumedDate)
  }

  object TransactionGroup {
    private type E = models.accounting.TransactionGroup

    val createdDate: ModelField[LocalDateTime, E] = new ModelField("createdDate", _.createdDate)
  }

  object BalanceCheck {
    private type E = models.accounting.BalanceCheck

    val issuerId: ModelField[Long, E] = new ModelField("issuerId", _.issuerId)
    val moneyReservoirCode: ModelField[String, E] =
      new ModelField("moneyReservoirCode", _.moneyReservoirCode)
    val balanceInCents: ModelField[Long, E] = new ModelField("balanceInCents", _.balanceInCents)
    val createdDate: ModelField[LocalDateTime, E] = new ModelField("createdDate", _.createdDate)
    val checkDate: ModelField[LocalDateTime, E] = new ModelField("checkDate", _.checkDate)
  }

  object ExchangeRateMeasurement {
    private type E = ExchangeRateMeasurement

    val date: ModelField[LocalDateTime, E] = new ModelField("date", _.date)
    val foreignCurrencyCode: ModelField[String, E] =
      new ModelField("foreignCurrencyCode", _.foreignCurrencyCode)
    val ratioReferenceToForeignCurrency: ModelField[Double, E] =
      new ModelField("ratioReferenceToForeignCurrency", _.ratioReferenceToForeignCurrency)
  }
}
