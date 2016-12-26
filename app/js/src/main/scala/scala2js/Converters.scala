package scala2js

import java.time.Month.AUGUST
import java.time.{LocalDate, LocalTime}

import api.ScalaJsApi.EntityType

import scala.scalajs.js
import models.accounting.Transaction
import common.time.LocalDateTime
import models.manager.Entity

import scala.collection.immutable.Seq
import scala2js.Scala2Js.Converter
import js.JSConverters._
import scala.collection.mutable

object Converters {

  // **************** Non-implicits **************** //
  def entityTypeToConverter(entityType: EntityType.Any): Scala2Js.MapConverter[entityType.get] = {
    ???
  }
  implicit def implicitEntityTypeToConverter[E <: Entity : EntityType]: Scala2Js.MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    entityTypeToConverter(entityType)
  }

  // **************** General converters **************** //
  implicit object NullConverter extends Scala2Js.Converter[js.Any] {
    override def toJs(obj: js.Any) = obj
    override def toScala(obj: js.Any) = obj
  }

  implicit object StringConverter extends Scala2Js.Converter[String] {
    override def toJs(string: String) = string
    override def toScala(value: js.Any) = value.asInstanceOf[String]
  }

  implicit def seqConverter[A](implicit elemConverter: Scala2Js.Converter[A]): Scala2Js.Converter[Seq[A]] =
    new Converter[Seq[A]] {
      override def toJs(seq: Seq[A]) =
        seq.toStream.map(elemConverter.toJs).toJSArray
      override def toScala(value: js.Any) =
        value.asInstanceOf[js.Array[js.Any]].toStream.map(elemConverter.toScala).toVector
    }

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

  // **************** Entity converters **************** //
  implicit object TransactionConverter extends Scala2Js.MapConverter[Transaction] {
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
    override def toScala(dict: js.Dictionary[js.Any]) = {
      val dateTimeConverter = implicitly[Scala2Js.Converter[LocalDateTime]]
      def getValue[T](key: String): T = {
        require(dict.contains(key), s"Key $key is missing from ${js.JSON.stringify(dict)}")
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
