package scala2js

import common.time.LocalDateTime
import models.manager.Entity

import scala.collection.immutable.Seq
import scala2js.Converters._

object Keys {
  def id[E <: Entity]: Scala2Js.Key[Long, E] = Scala2Js.Key[Long, E]("id")

  object User {
    private type E = models.User

    val loginName = Scala2Js.Key[String, E]("loginName")
    val passwordHash = Scala2Js.Key[String, E]("passwordHash")
    val name = Scala2Js.Key[String, E]("name")
    val databaseEncryptionKey = Scala2Js.Key[String, E]("databaseEncryptionKey")
  }

  object Transaction {
    private type E = models.accounting.Transaction

    val transactionGroupId = Scala2Js.Key[Long, E]("transactionGroupId")
    val issuerId = Scala2Js.Key[Long, E]("issuerId")
    val beneficiaryAccountCode = Scala2Js.Key[String, E]("beneficiaryAccountCode")
    val moneyReservoirCode = Scala2Js.Key[String, E]("moneyReservoirCode")
    val categoryCode = Scala2Js.Key[String, E]("categoryCode")
    val description = Scala2Js.Key[String, E]("description")
    val flowInCents = Scala2Js.Key[Long, E]("flowInCents")
    val detailDescription = Scala2Js.Key[String, E]("detailDescription")
    val tags = Scala2Js.Key[Seq[String], E]("tags")
    val createdDate = Scala2Js.Key[LocalDateTime, E]("createdDate")
    val transactionDate = Scala2Js.Key[LocalDateTime, E]("transactionDate")
    val consumedDate = Scala2Js.Key[LocalDateTime, E]("consumedDate")
  }

  object TransactionGroup {
    private type E = models.accounting.TransactionGroup

    val createdDate = Scala2Js.Key[LocalDateTime, E]("createdDate")
  }

  object BalanceCheck {
    private type E = models.accounting.BalanceCheck

    val issuerId = Scala2Js.Key[Long, E]("issuerId")
    val moneyReservoirCode = Scala2Js.Key[String, E]("moneyReservoirCode")
    val balanceInCents = Scala2Js.Key[Long, E]("balanceInCents")
    val createdDate = Scala2Js.Key[LocalDateTime, E]("createdDate")
    val checkDate = Scala2Js.Key[LocalDateTime, E]("checkDate")
  }

  object ExchangeRateMeasurement {
    private type E = models.accounting.money.ExchangeRateMeasurement

    val date = Scala2Js.Key[LocalDateTime, E]("date")
    val foreignCurrencyCode = Scala2Js.Key[String, E]("foreignCurrencyCode")
    val ratioReferenceToForeignCurrency = Scala2Js.Key[Double, E]("ratioReferenceToForeignCurrency")
  }
}
