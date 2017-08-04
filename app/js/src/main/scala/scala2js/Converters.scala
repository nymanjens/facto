package scala2js

import java.time.{LocalDate, LocalTime}

import common.time.LocalDateTime
import models._
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{EntityType, _}

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala2js.Scala2Js.Converter

object Converters {

  // **************** Non-implicits **************** //
  implicit def entityTypeToConverter[E <: Entity: EntityType]: Scala2Js.MapConverter[E] = {
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

  implicit object LongConverter extends Scala2Js.Converter[Long] {
    override def toJs(long: Long) = {
      // Note: It would be easier to implement this by `"%022d".format(long)`
      // but that transforms the given long to a javascript number (double precision)
      // causing the least significatant long digits sometimes to become zero
      // (e.g. 6886911427549585292 becomes 6886911427549585000)
      val signChar = if (long < 0) "-" else ""
      val stringWithoutSign = Math.abs(long).toString

      val numZerosToPrepend = 22 - stringWithoutSign.size
      require(numZerosToPrepend > 0)
      signChar + ("0" * numZerosToPrepend) + stringWithoutSign
    }
    override def toScala(value: js.Any) = value.asInstanceOf[String].toLong
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
      LocalDateTime.of(LocalDate.ofEpochDay(epochDay), LocalTime.ofSecondOfDay(secondOfDay))
    }
  }

  // **************** Entity converters **************** //
  private[scala2js] abstract class EntityConverter[E <: Entity] extends Scala2Js.MapConverter[E] {
    override final def toJs(entity: E) = {
      val result = toJsWithoutId(entity)
      for (id <- entity.idOption) {
        val pair = Scala2Js.Key.toJsPair(Keys.id -> id)
        result.update(pair._1, pair._2)
      }
      result
    }

    override final def toScala(dict: js.Dictionary[js.Any]) = {
      val entityWithoutId = toScalaWithoutId(dict)
      val idOption = getOptionalValueFromDict[String](dict)("id").map(Scala2Js.toScala[Long](_))
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
    override def toJsWithoutId(entity: User) = {
      js.Dictionary[js.Any](
        Scala2Js.Key.toJsPair(Keys.User.loginName -> entity.loginName),
        Scala2Js.Key.toJsPair(Keys.User.passwordHash -> entity.passwordHash),
        Scala2Js.Key.toJsPair(Keys.User.name -> entity.name)
      )
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: Scala2Js.Key[T, User]) =
        getRequiredValueFromDict(dict)(key)

      User(
        loginName = getRequired(Keys.User.loginName),
        passwordHash = getRequired(Keys.User.passwordHash),
        name = getRequired(Keys.User.name))
    }
  }

  implicit object TransactionConverter extends EntityConverter[Transaction] {
    override def toJsWithoutId(entity: Transaction) = {
      js.Dictionary[js.Any](
        Scala2Js.Key.toJsPair(Keys.Transaction.transactionGroupId -> entity.transactionGroupId),
        Scala2Js.Key.toJsPair(Keys.Transaction.issuerId -> entity.issuerId),
        Scala2Js.Key.toJsPair(Keys.Transaction.beneficiaryAccountCode -> entity.beneficiaryAccountCode),
        Scala2Js.Key.toJsPair(Keys.Transaction.moneyReservoirCode -> entity.moneyReservoirCode),
        Scala2Js.Key.toJsPair(Keys.Transaction.categoryCode -> entity.categoryCode),
        Scala2Js.Key.toJsPair(Keys.Transaction.description -> entity.description),
        Scala2Js.Key.toJsPair(Keys.Transaction.flowInCents -> entity.flowInCents),
        Scala2Js.Key.toJsPair(Keys.Transaction.detailDescription -> entity.detailDescription),
        Scala2Js.Key.toJsPair(Keys.Transaction.tagsString -> entity.tagsString),
        Scala2Js.Key.toJsPair(Keys.Transaction.createdDate -> entity.createdDate),
        Scala2Js.Key.toJsPair(Keys.Transaction.transactionDate -> entity.transactionDate),
        Scala2Js.Key.toJsPair(Keys.Transaction.consumedDate -> entity.consumedDate)
      )
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: Scala2Js.Key[T, Transaction]) =
        getRequiredValueFromDict(dict)(key)

      Transaction(
        transactionGroupId = getRequired(Keys.Transaction.transactionGroupId),
        issuerId = getRequired(Keys.Transaction.issuerId),
        beneficiaryAccountCode = getRequired(Keys.Transaction.beneficiaryAccountCode),
        moneyReservoirCode = getRequired(Keys.Transaction.moneyReservoirCode),
        categoryCode = getRequired(Keys.Transaction.categoryCode),
        description = getRequired(Keys.Transaction.description),
        flowInCents = getRequired(Keys.Transaction.flowInCents),
        detailDescription = getRequired(Keys.Transaction.detailDescription),
        tagsString = getRequired(Keys.Transaction.tagsString),
        createdDate = getRequired(Keys.Transaction.createdDate),
        transactionDate = getRequired(Keys.Transaction.transactionDate),
        consumedDate = getRequired(Keys.Transaction.consumedDate)
      )
    }
  }

  implicit object TransactionGroupConverter extends EntityConverter[TransactionGroup] {
    override def toJsWithoutId(entity: TransactionGroup) = {
      js.Dictionary[js.Any](Scala2Js.Key.toJsPair(Keys.TransactionGroup.createdDate -> entity.createdDate))
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: Scala2Js.Key[T, TransactionGroup]) =
        getRequiredValueFromDict(dict)(key)

      TransactionGroup(createdDate = getRequired(Keys.TransactionGroup.createdDate))
    }
  }

  implicit object BalanceCheckConverter extends EntityConverter[BalanceCheck] {
    override def toJsWithoutId(entity: BalanceCheck) = {
      js.Dictionary[js.Any](
        Scala2Js.Key.toJsPair(Keys.BalanceCheck.issuerId -> entity.issuerId),
        Scala2Js.Key.toJsPair(Keys.BalanceCheck.moneyReservoirCode -> entity.moneyReservoirCode),
        Scala2Js.Key.toJsPair(Keys.BalanceCheck.balanceInCents -> entity.balanceInCents),
        Scala2Js.Key.toJsPair(Keys.BalanceCheck.createdDate -> entity.createdDate),
        Scala2Js.Key.toJsPair(Keys.BalanceCheck.checkDate -> entity.checkDate)
      )
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: Scala2Js.Key[T, BalanceCheck]) =
        getRequiredValueFromDict(dict)(key)

      BalanceCheck(
        issuerId = getRequired(Keys.BalanceCheck.issuerId),
        moneyReservoirCode = getRequired(Keys.BalanceCheck.moneyReservoirCode),
        balanceInCents = getRequired(Keys.BalanceCheck.balanceInCents),
        createdDate = getRequired(Keys.BalanceCheck.createdDate),
        checkDate = getRequired(Keys.BalanceCheck.checkDate)
      )
    }
  }

  implicit object ExchangeRateMeasurementConverter extends EntityConverter[ExchangeRateMeasurement] {
    override def toJsWithoutId(entity: ExchangeRateMeasurement) = {
      js.Dictionary[js.Any](
        Scala2Js.Key.toJsPair(Keys.ExchangeRateMeasurement.date -> entity.date),
        Scala2Js.Key.toJsPair(
          Keys.ExchangeRateMeasurement.foreignCurrencyCode -> entity.foreignCurrencyCode),
        Scala2Js.Key.toJsPair(
          Keys.ExchangeRateMeasurement.ratioReferenceToForeignCurrency -> entity.ratioReferenceToForeignCurrency)
      )
    }
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: Scala2Js.Key[T, ExchangeRateMeasurement]) =
        getRequiredValueFromDict(dict)(key)

      ExchangeRateMeasurement(
        date = getRequired(Keys.ExchangeRateMeasurement.date),
        foreignCurrencyCode = getRequired(Keys.ExchangeRateMeasurement.foreignCurrencyCode),
        ratioReferenceToForeignCurrency =
          getRequired(Keys.ExchangeRateMeasurement.ratioReferenceToForeignCurrency)
      )
    }
  }
}
