package scala2js

import common.time.LocalDateTime

import scala2js.Converters._

object Keys {
  val id = Scala2Js.Key[Long]("id")

  object User {
    val loginName = Scala2Js.Key[String]("loginName")
    val passwordHash = Scala2Js.Key[String]("passwordHash")
    val name = Scala2Js.Key[String]("name")
  }

  object Transaction {
    val transactionGroupId = Scala2Js.Key[Long]("transactionGroupId")
    val issuerId = Scala2Js.Key[Long]("issuerId")
    val beneficiaryAccountCode = Scala2Js.Key[String]("beneficiaryAccountCode")
    val moneyReservoirCode = Scala2Js.Key[String]("moneyReservoirCode")
    val categoryCode = Scala2Js.Key[String]("categoryCode")
    val description = Scala2Js.Key[String]("description")
    val flowInCents = Scala2Js.Key[Long]("flowInCents")
    val detailDescription = Scala2Js.Key[String]("detailDescription")
    val tagsString = Scala2Js.Key[String]("tagsString")
    val createdDate = Scala2Js.Key[LocalDateTime]("createdDate")
    val transactionDate = Scala2Js.Key[LocalDateTime]("transactionDate")
    val consumedDate = Scala2Js.Key[LocalDateTime]("consumedDate")
  }

  object TransactionGroup {
    val createdDate = Scala2Js.Key[LocalDateTime]("createdDate")
  }

  object BalanceCheck {
    val issuerId = Scala2Js.Key[Long]("issuerId")
    val moneyReservoirCode = Scala2Js.Key[String]("moneyReservoirCode")
    val balanceInCents = Scala2Js.Key[Long]("balanceInCents")
    val createdDate = Scala2Js.Key[LocalDateTime]("createdDate")
    val checkDate = Scala2Js.Key[LocalDateTime]("checkDate")
  }

  object ExchangeRateMeasurement {
    val date = Scala2Js.Key[LocalDateTime]("date")
    val foreignCurrencyCode = Scala2Js.Key[String]("foreignCurrencyCode")
    val ratioReferenceToForeignCurrency = Scala2Js.Key[Double]("ratioReferenceToForeignCurrency")
  }
}
