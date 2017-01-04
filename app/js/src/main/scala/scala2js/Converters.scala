package scala2js

import java.time.Month.AUGUST
import java.time.{LocalDate, LocalTime}

import models.manager.EntityType

import scala.scalajs.js
import models.accounting._
import common.time.LocalDateTime
import models._
import models.accounting.money.ExchangeRateMeasurement
import models.manager._

import scala.collection.immutable.Seq
import scala2js.Scala2Js.Converter
import js.JSConverters._
import scala.collection.mutable

object Converters {

  // **************** Non-implicits **************** //
  implicit def entityTypeToConverter[E <: Entity : EntityType]: Scala2Js.MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: Scala2Js.MapConverter[_ <: Entity] = entityType match {
      case EntityType.UserType => UserConverter
      case EntityType.TransactionType => TransactionConverter
      case EntityType.TransactionGroupType => TransactionGroupConverter
      case EntityType.BalanceCheckType => BalanceCheckConverter
      case EntityType.ExchangeRateMeasurementType => ExchangeRateMeasurementConverter
    }
    converter.asInstanceOf[Scala2Js.MapConverter[E]]
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

  implicit object IntConverter extends Scala2Js.Converter[Int] {
    override def toJs(int: Int) = int
    override def toScala(value: js.Any) = value.asInstanceOf[Int]
  }

  implicit object DoubleConverter extends Scala2Js.Converter[Double] {
    override def toJs(double: Double) = double
    override def toScala(value: js.Any) = value.asInstanceOf[Double]
  }

  implicit def seqConverter[A: Scala2Js.Converter]: Scala2Js.Converter[Seq[A]] =
    new Converter[Seq[A]] {
      override def toJs(seq: Seq[A]) =
        seq.toStream.map(Scala2Js.toJs[A]).toJSArray
      override def toScala(value: js.Any) =
        value.asInstanceOf[js.Array[js.Any]].toStream.map(Scala2Js.toScala[A]).toVector
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
        LocalTime.ofSecondOfDay(secondOfDay))
    }
  }

  // **************** Entity converters **************** //
  private[scala2js] abstract class EntityConverter[E <: Entity] extends Scala2Js.MapConverter[E] {
    override final def toJs(entity: E) = {
      val result = toJsWithoutId(entity)
      for (id <- entity.idOption) {
        result.update("id", id.toString)
      }
      result
    }

    override final def toScala(dict: js.Dictionary[js.Any]) = {
      val entityWithoutId = toScalaWithoutId(dict)
      val idOption = getOptionalValueFromDict[String](dict)("id").map(_.toLong)
      if (idOption.isDefined) {
        entityWithoutId.withId(idOption.get).asInstanceOf[E]
      } else {
        entityWithoutId
      }
    }

    protected def toJsWithoutId(entity: E): js.Dictionary[js.Any]
    protected def toScalaWithoutId(dict: js.Dictionary[js.Any]): E
  }

  implicit object UserConverter extends EntityConverter[User] {
    override def toJsWithoutId(user: User) = {
      js.Dictionary[js.Any](
        "loginName" -> user.loginName,
        "passwordHash" -> user.passwordHash,
        "name" -> user.name)
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)

      User(
        loginName = getRequired[String]("loginName"),
        passwordHash = getRequired[String]("passwordHash"),
        name = getRequired[String]("name"))
    }
  }

  implicit object TransactionConverter extends EntityConverter[Transaction] {
    override def toJsWithoutId(transaction: Transaction) = {
      js.Dictionary[js.Any](
        "transactionGroupId" -> transaction.transactionGroupId.toString,
        "issuerId" -> transaction.issuerId.toString,
        "beneficiaryAccountCode" -> transaction.beneficiaryAccountCode,
        "moneyReservoirCode" -> transaction.moneyReservoirCode,
        "categoryCode" -> transaction.categoryCode,
        "description" -> transaction.description,
        "flowInCents" -> transaction.flowInCents.toString,
        "detailDescription" -> transaction.detailDescription,
        "tagsString" -> transaction.tagsString,
        "createdDate" -> Scala2Js.toJs(transaction.createdDate),
        "transactionDate" -> Scala2Js.toJs(transaction.transactionDate),
        "consumedDate" -> Scala2Js.toJs(transaction.consumedDate))
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)

      Transaction(
        transactionGroupId = getRequired[String]("transactionGroupId").toLong,
        issuerId = getRequired[String]("issuerId").toLong,
        beneficiaryAccountCode = getRequired[String]("beneficiaryAccountCode"),
        moneyReservoirCode = getRequired[String]("moneyReservoirCode"),
        categoryCode = getRequired[String]("categoryCode"),
        description = getRequired[String]("description"),
        flowInCents = getRequired[String]("flowInCents").toLong,
        detailDescription = getRequired[String]("detailDescription"),
        tagsString = getRequired[String]("tagsString"),
        createdDate = getRequired[LocalDateTime]("createdDate"),
        transactionDate = getRequired[LocalDateTime]("transactionDate"),
        consumedDate = getRequired[LocalDateTime]("consumedDate"))
    }
  }

  implicit object TransactionGroupConverter extends EntityConverter[TransactionGroup] {
    override def toJsWithoutId(transactionGroup: TransactionGroup) = {
      js.Dictionary[js.Any]("createdDate" -> Scala2Js.toJs(transactionGroup.createdDate))
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)

      TransactionGroup(createdDate = getRequired[LocalDateTime]("createdDate"))
    }
  }

  implicit object BalanceCheckConverter extends EntityConverter[BalanceCheck] {
    override def toJsWithoutId(balanceCheck: BalanceCheck) = {
      js.Dictionary[js.Any](
        "issuerId" -> balanceCheck.issuerId.toString,
        "moneyReservoirCode" -> balanceCheck.moneyReservoirCode,
        "balanceInCents" -> balanceCheck.balanceInCents.toString,
        "createdDate" -> Scala2Js.toJs(balanceCheck.createdDate),
        "checkDate" -> Scala2Js.toJs(balanceCheck.checkDate))
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)

      BalanceCheck(
        issuerId = getRequired[String]("issuerId").toLong,
        moneyReservoirCode = getRequired[String]("moneyReservoirCode"),
        balanceInCents = getRequired[String]("balanceInCents").toLong,
        createdDate = getRequired[LocalDateTime]("createdDate"),
        checkDate = getRequired[LocalDateTime]("checkDate"))
    }
  }

  implicit object ExchangeRateMeasurementConverter extends EntityConverter[ExchangeRateMeasurement] {
    override def toJsWithoutId(exchangeRateMeasurement: ExchangeRateMeasurement) = {
      js.Dictionary[js.Any](
        "date" -> Scala2Js.toJs(exchangeRateMeasurement.date),
        "foreignCurrencyCode" -> exchangeRateMeasurement.foreignCurrencyCode,
        "ratioReferenceToForeignCurrency" -> exchangeRateMeasurement.ratioReferenceToForeignCurrency)
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)

      ExchangeRateMeasurement(
        date = getRequired[LocalDateTime]("date"),
        foreignCurrencyCode = getRequired[String]("foreignCurrencyCode"),
        ratioReferenceToForeignCurrency = getRequired[Double]("ratioReferenceToForeignCurrency"))
    }
  }
}
