package scala2js

import java.time.{LocalDate, LocalTime}

import common.time.LocalDateTime
import models._
import models.access.{Fields, ModelField}
import models.accounting._
import models.modification._
import models.money.ExchangeRateMeasurement
import models.user.User

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

  def fromModelField[V](modelField: ModelField[V, _]): Converter[V] = {
    def fromField[V2: Converter](modelField: ModelField[V2, _]): Converter[V2] = implicitly[Converter[V2]]
    val result = modelField match {
      case Fields.User.loginName => fromField(Fields.User.loginName)
      case Fields.User.passwordHash => fromField(Fields.User.passwordHash)
      case Fields.User.name => fromField(Fields.User.name)
      case Fields.User.databaseEncryptionKey => fromField(Fields.User.databaseEncryptionKey)
      case Fields.User.expandCashFlowTablesByDefault => fromField(Fields.User.expandCashFlowTablesByDefault)
      case Fields.Transaction.transactionGroupId => fromField(Fields.Transaction.transactionGroupId)
      case Fields.Transaction.issuerId => fromField(Fields.Transaction.issuerId)
      case Fields.Transaction.beneficiaryAccountCode => fromField(Fields.Transaction.beneficiaryAccountCode)
      case Fields.Transaction.moneyReservoirCode => fromField(Fields.Transaction.moneyReservoirCode)
      case Fields.Transaction.categoryCode => fromField(Fields.Transaction.categoryCode)
      case Fields.Transaction.description => fromField(Fields.Transaction.description)
      case Fields.Transaction.flowInCents => fromField(Fields.Transaction.flowInCents)
      case Fields.Transaction.detailDescription => fromField(Fields.Transaction.detailDescription)
      case Fields.Transaction.tags => fromField(Fields.Transaction.tags)
      case Fields.Transaction.createdDate => fromField(Fields.Transaction.createdDate)
      case Fields.Transaction.transactionDate => fromField(Fields.Transaction.transactionDate)
      case Fields.Transaction.consumedDate => fromField(Fields.Transaction.consumedDate)
      case Fields.TransactionGroup.createdDate => fromField(Fields.TransactionGroup.createdDate)
      case Fields.BalanceCheck.issuerId => fromField(Fields.BalanceCheck.issuerId)
      case Fields.BalanceCheck.moneyReservoirCode => fromField(Fields.BalanceCheck.moneyReservoirCode)
      case Fields.BalanceCheck.balanceInCents => fromField(Fields.BalanceCheck.balanceInCents)
      case Fields.BalanceCheck.createdDate => fromField(Fields.BalanceCheck.createdDate)
      case Fields.BalanceCheck.checkDate => fromField(Fields.BalanceCheck.checkDate)
      case Fields.ExchangeRateMeasurement.date => fromField(Fields.ExchangeRateMeasurement.date)
      case Fields.ExchangeRateMeasurement.foreignCurrencyCode =>
        fromField(Fields.ExchangeRateMeasurement.foreignCurrencyCode)
      case Fields.ExchangeRateMeasurement.ratioReferenceToForeignCurrency =>
        fromField(Fields.ExchangeRateMeasurement.ratioReferenceToForeignCurrency)
      case f if f.name == "id" => LongConverter
    }
    result.asInstanceOf[Converter[V]]
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

  implicit object BooleanConverter extends Scala2Js.Converter[Boolean] {
    override def toJs(bool: Boolean) = bool
    override def toScala(value: js.Any) = value.asInstanceOf[Boolean]
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
      val result = js.Dictionary[js.Any]()

      def addField[V](field: ModelField[V, E]): Unit = {
        val (fieldName, jsValue) = Scala2Js.toJsPair(field -> field.get(entity))
        result.update(fieldName, jsValue)
      }
      for (field <- allFieldsWithoutId) {
        addField(field)
      }
      for (id <- entity.idOption) {
        val (fieldName, jsValue) = Scala2Js.toJsPair(Fields.id -> id)
        result.update(fieldName, jsValue)
      }
      result
    }

    override final def toScala(dict: js.Dictionary[js.Any]) = {
      val entityWithoutId = toScalaWithoutId(dict)
      val idOption = dict.get("id").map(Scala2Js.toScala[Long])
      if (idOption.isDefined) {
        entityWithoutId.withId(idOption.get).asInstanceOf[E]
      } else {
        entityWithoutId
      }
    }

    protected def allFieldsWithoutId: Seq[ModelField[_, E]]
    protected def toScalaWithoutId(dict: js.Dictionary[js.Any]): E
  }

  implicit object UserConverter extends EntityConverter[User] {
    override def allFieldsWithoutId =
      Seq(
        Fields.User.loginName,
        Fields.User.passwordHash,
        Fields.User.name,
        Fields.User.databaseEncryptionKey,
        Fields.User.expandCashFlowTablesByDefault)

    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, User]) =
        getRequiredValueFromDict(dict)(field)

      User(
        loginName = getRequired(Fields.User.loginName),
        passwordHash = getRequired(Fields.User.passwordHash),
        name = getRequired(Fields.User.name),
        databaseEncryptionKey = getRequired(Fields.User.databaseEncryptionKey),
        expandCashFlowTablesByDefault = getRequired(Fields.User.expandCashFlowTablesByDefault)
      )
    }
  }

  implicit object TransactionConverter extends EntityConverter[Transaction] {
    override def allFieldsWithoutId =
      Seq(
        Fields.Transaction.transactionGroupId,
        Fields.Transaction.issuerId,
        Fields.Transaction.beneficiaryAccountCode,
        Fields.Transaction.moneyReservoirCode,
        Fields.Transaction.categoryCode,
        Fields.Transaction.description,
        Fields.Transaction.flowInCents,
        Fields.Transaction.detailDescription,
        Fields.Transaction.tags,
        Fields.Transaction.createdDate,
        Fields.Transaction.transactionDate,
        Fields.Transaction.consumedDate
      )

    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, Transaction]) =
        getRequiredValueFromDict(dict)(field)

      Transaction(
        transactionGroupId = getRequired(Fields.Transaction.transactionGroupId),
        issuerId = getRequired(Fields.Transaction.issuerId),
        beneficiaryAccountCode = getRequired(Fields.Transaction.beneficiaryAccountCode),
        moneyReservoirCode = getRequired(Fields.Transaction.moneyReservoirCode),
        categoryCode = getRequired(Fields.Transaction.categoryCode),
        description = getRequired(Fields.Transaction.description),
        flowInCents = getRequired(Fields.Transaction.flowInCents),
        detailDescription = getRequired(Fields.Transaction.detailDescription),
        tags = getRequired(Fields.Transaction.tags),
        createdDate = getRequired(Fields.Transaction.createdDate),
        transactionDate = getRequired(Fields.Transaction.transactionDate),
        consumedDate = getRequired(Fields.Transaction.consumedDate)
      )
    }
  }

  implicit object TransactionGroupConverter extends EntityConverter[TransactionGroup] {
    override def allFieldsWithoutId = Seq(Fields.TransactionGroup.createdDate)
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, TransactionGroup]) =
        getRequiredValueFromDict(dict)(field)

      TransactionGroup(createdDate = getRequired(Fields.TransactionGroup.createdDate))
    }
  }

  implicit object BalanceCheckConverter extends EntityConverter[BalanceCheck] {
    override def allFieldsWithoutId =
      Seq(
        Fields.BalanceCheck.issuerId,
        Fields.BalanceCheck.moneyReservoirCode,
        Fields.BalanceCheck.balanceInCents,
        Fields.BalanceCheck.createdDate,
        Fields.BalanceCheck.checkDate
      )
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, BalanceCheck]) =
        getRequiredValueFromDict(dict)(field)

      BalanceCheck(
        issuerId = getRequired(Fields.BalanceCheck.issuerId),
        moneyReservoirCode = getRequired(Fields.BalanceCheck.moneyReservoirCode),
        balanceInCents = getRequired(Fields.BalanceCheck.balanceInCents),
        createdDate = getRequired(Fields.BalanceCheck.createdDate),
        checkDate = getRequired(Fields.BalanceCheck.checkDate)
      )
    }
  }

  implicit object ExchangeRateMeasurementConverter extends EntityConverter[ExchangeRateMeasurement] {
    override def allFieldsWithoutId =
      Seq(
        Fields.ExchangeRateMeasurement.date,
        Fields.ExchangeRateMeasurement.foreignCurrencyCode,
        Fields.ExchangeRateMeasurement.ratioReferenceToForeignCurrency)
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, ExchangeRateMeasurement]) =
        getRequiredValueFromDict(dict)(field)

      ExchangeRateMeasurement(
        date = getRequired(Fields.ExchangeRateMeasurement.date),
        foreignCurrencyCode = getRequired(Fields.ExchangeRateMeasurement.foreignCurrencyCode),
        ratioReferenceToForeignCurrency =
          getRequired(Fields.ExchangeRateMeasurement.ratioReferenceToForeignCurrency)
      )
    }
  }
}
