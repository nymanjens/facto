package scala2js

import java.time.Month.AUGUST
import java.time.{LocalDate, LocalTime}

import scala.scalajs.js

import models.accounting.Transaction
import common.time.LocalDateTime

object Converters {

  implicit object LocalDateTimeConverter extends Scala2Js.Converter[LocalDateTime] {

    private val secondsInDay = 60 * 60 * 24

    override def toJs(dateTime: LocalDateTime) = {
      val epochDay = dateTime.toLocalDate.toEpochDay.toInt
      val secondOfDay = dateTime.toLocalTime.toSecondOfDay
      epochDay * secondsInDay + secondOfDay
    }
    override def toScala(value: js.Any) = {
      val combinedInt = value.asInstanceOf[Int]
      val epochDay = combinedInt / secondsInDay
      val secondOfDay = combinedInt % secondsInDay
      LocalDateTime.of(
        LocalDate.ofEpochDay(epochDay),
        LocalTime.ofNanoOfDay(secondOfDay))
    }
  }

  implicit object TransactionConverter extends Scala2Js.Converter[Transaction] {
    override def toJs(transaction: Transaction) = {
      val dateTimeConverter = implicitly[Scala2Js.Converter[LocalDateTime]]
      val result = js.Dictionary[js.Any](
        "transactionGroupId" -> transaction.transactionGroupId.toInt,
        "issuerId" -> transaction.issuerId.toInt,
        "beneficiaryAccountCode" -> transaction.beneficiaryAccountCode,
        "moneyReservoirCode" -> transaction.moneyReservoirCode,
        "categoryCode" -> transaction.categoryCode,
        "description" -> transaction.description,
        "flowInCents" -> transaction.flowInCents.toString,
        "detailDescription" -> transaction.detailDescription,
        "tagsString" -> transaction.tagsString,
        "createdDate" -> dateTimeConverter.toJs(transaction.createdDate),
        "transactionDate" -> dateTimeConverter.toJs(transaction.transactionDate),
        "consumedDate" -> dateTimeConverter.toJs(transaction.consumedDate)
      )
      for (id <- transaction.idOption) {
        result.update("id", id.toInt)
      }
      result
    }
    override def toScala(value: js.Any) = {
      val dict = value.asInstanceOf[js.Dictionary[js.Any]]
      val dateTimeConverter = implicitly[Scala2Js.Converter[LocalDateTime]]
      def getValue[T](key: String): T = {
        require(dict.contains(key), s"Key $key is missing from ${js.JSON.stringify(value)}")
        dict(key).asInstanceOf[T]
      }
      def getString(key: String): String = getValue[String](key)
      def getLong(key: String): Long = getValue[Int](key).toLong
      def getDate(key: String): LocalDateTime = dateTimeConverter.toScala(getValue[js.Any](key))

      Transaction(
        transactionGroupId = getLong("transactionGroupId"),
        issuerId = getLong("issuerId"),
        beneficiaryAccountCode = getString("beneficiaryAccountCode"),
        moneyReservoirCode = getString("moneyReservoirCode"),
        categoryCode = getString("categoryCode"),
        description = getString("description"),
        flowInCents = getString("flowInCents").toLong,
        detailDescription = getString("detailDescription"),
        tagsString = getString("tagsString"),
        createdDate = getDate("createdDate"),
        transactionDate = getDate("transactionDate"),
        consumedDate = getDate("consumedDate"),
        idOption = dict.get("id").map(_.asInstanceOf[Int].toLong))
    }
  }
}
