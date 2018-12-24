package scala2js

import java.time.LocalDate
import java.time.LocalTime

import common.GuavaReplacement.ImmutableBiMap
import common.time.LocalDateTime
import models._
import models.access.ModelField
import models.accounting._
import models.modification._
import models.money.ExchangeRateMeasurement
import models.user.User
import scala2js.Scala2Js.Converter
import scala2js.Scala2Js.MapConverter

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object Converters {

  // **************** Convertor generators **************** //
  implicit def fromEntityType[E <: Entity: EntityType]: MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: MapConverter[_ <: Entity] = entityType match {
      case EntityType.UserType                    => UserConverter
      case EntityType.TransactionType             => TransactionConverter
      case EntityType.TransactionGroupType        => TransactionGroupConverter
      case EntityType.BalanceCheckType            => BalanceCheckConverter
      case EntityType.ExchangeRateMeasurementType => ExchangeRateMeasurementConverter
    }
    converter.asInstanceOf[MapConverter[E]]
  }

  def fromModelField[V](modelField: ModelField[V, _]): Converter[V] = {
    def fromType[V2: Converter](fieldType: ModelField.FieldType[V2]): Converter[V2] = implicitly
    val result = modelField.fieldType match {
      case ModelField.FieldType.BooleanType       => fromType(ModelField.FieldType.BooleanType)
      case ModelField.FieldType.LongType          => fromType(ModelField.FieldType.LongType)
      case ModelField.FieldType.DoubleType        => fromType(ModelField.FieldType.DoubleType)
      case ModelField.FieldType.StringType        => fromType(ModelField.FieldType.StringType)
      case ModelField.FieldType.LocalDateTimeType => fromType(ModelField.FieldType.LocalDateTimeType)
      case ModelField.FieldType.StringSeqType     => fromType(ModelField.FieldType.StringSeqType)
    }
    result.asInstanceOf[Converter[V]]
  }

  def enumConverter[T](values: T*): Converter[T] = {
    val valueToNumber: ImmutableBiMap[T, Int] = {
      val builder = ImmutableBiMap.builder[T, Int]()
      for ((value, number) <- values.zipWithIndex) {
        builder.put(value, number)
      }
      builder.build()
    }

    new Converter[T] {
      override def toJs(value: T) = Scala2Js.toJs(valueToNumber.get(value))
      override def toScala(value: js.Any) = valueToNumber.inverse().get(Scala2Js.toScala[Int](value))
    }
  }

  implicit def seqConverter[A: Converter]: Converter[Seq[A]] =
    new Converter[Seq[A]] {
      override def toJs(seq: Seq[A]) =
        seq.toStream.map(Scala2Js.toJs[A]).toJSArray
      override def toScala(value: js.Any) =
        value.asInstanceOf[js.Array[js.Any]].toStream.map(Scala2Js.toScala[A]).toVector
    }

  // **************** General converters **************** //
  implicit object NullConverter extends Converter[js.Any] {
    override def toJs(obj: js.Any) = obj
    override def toScala(obj: js.Any) = obj
  }

  implicit object StringConverter extends Converter[String] {
    override def toJs(string: String) = string
    override def toScala(value: js.Any) = value.asInstanceOf[String]
  }

  implicit object BooleanConverter extends Converter[Boolean] {
    override def toJs(bool: Boolean) = bool
    override def toScala(value: js.Any) = value.asInstanceOf[Boolean]
  }

  implicit object IntConverter extends Converter[Int] {
    override def toJs(int: Int) = int
    override def toScala(value: js.Any) = value.asInstanceOf[Int]
  }

  implicit object LongConverter extends Converter[Long] {
    override def toJs(long: Long) = {
      // Note: It would be easier to implement this by `"%022d".format(long)`
      // but that transforms the given long to a javascript number (double precision)
      // causing the least significant long digits sometimes to become zero
      // (e.g. 6886911427549585292 becomes 6886911427549585000)
      val signChar = if (long < 0) "-" else ""
      val stringWithoutSign = Math.abs(long).toString

      val numZerosToPrepend = 22 - stringWithoutSign.size
      require(numZerosToPrepend > 0)
      signChar + ("0" * numZerosToPrepend) + stringWithoutSign
    }
    override def toScala(value: js.Any) = value.asInstanceOf[String].toLong
  }

  implicit object DoubleConverter extends Converter[Double] {
    override def toJs(double: Double) = double
    override def toScala(value: js.Any) = value.asInstanceOf[Double]
  }

  implicit object LocalDateTimeConverter extends Converter[LocalDateTime] {

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

  implicit val EntityTypeConverter: Converter[EntityType.any] = enumConverter(
    EntityType.UserType,
    EntityType.TransactionType,
    EntityType.TransactionGroupType,
    EntityType.BalanceCheckType,
    EntityType.ExchangeRateMeasurementType)

  implicit object EntityModificationConverter extends Converter[EntityModification] {
    private val addNumber: Int = 1
    private val updateNumber: Int = 2
    private val removeNumber: Int = 3

    override def toJs(modification: EntityModification) = {
      def internal[E <: Entity] = {
        val result = js.Array[js.Any]()

        result.push(Scala2Js.toJs[EntityType.any](modification.entityType))
        modification match {
          case EntityModification.Add(entity) =>
            result.push(addNumber)
            result.push(
              Scala2Js.toJs(entity.asInstanceOf[E])(
                fromEntityType(modification.entityType.asInstanceOf[EntityType[E]])))
          case EntityModification.Update(entity) =>
            result.push(updateNumber)
            result.push(
              Scala2Js.toJs(entity.asInstanceOf[E])(
                fromEntityType(modification.entityType.asInstanceOf[EntityType[E]])))
          case EntityModification.Remove(entityId) =>
            result.push(removeNumber)
            result.push(Scala2Js.toJs(entityId))
        }

        result
      }
      internal
    }

    override def toScala(value: js.Any) = {
      def internal[E <: Entity] = {
        val array = value.asInstanceOf[js.Array[js.Any]]
        implicit val entityType = Scala2Js.toScala[EntityType.any](array.shift()).asInstanceOf[EntityType[E]]
        val modificationTypeNumber = Scala2Js.toScala[Int](array.shift())

        array.toVector match {
          case Vector(entity) if modificationTypeNumber == addNumber =>
            EntityModification.Add(Scala2Js.toScala[E](entity))
          case Vector(entity) if modificationTypeNumber == updateNumber =>
            EntityModification.Update(Scala2Js.toScala[E](entity))
          case Vector(entityId) if modificationTypeNumber == removeNumber =>
            EntityModification.Remove(Scala2Js.toScala[Long](entityId))(entityType)
        }
      }
      internal
    }
  }

  // **************** Entity converters **************** //
  private[scala2js] abstract class EntityConverter[E <: Entity: EntityType] extends MapConverter[E] {
    override final def toJs(entity: E) = {
      val result = js.Dictionary[js.Any]()

      def addField[V](field: ModelField[V, E]): Unit = {
        result.update(field.name, Scala2Js.toJs(field.get(entity), field))
      }
      for (field <- allFieldsWithoutId) {
        addField(field)
      }
      for (id <- entity.idOption) {
        result.update(ModelField.id[E].name, Scala2Js.toJs(id, ModelField.id[E]))
      }
      result
    }

    override final def toScala(dict: js.Dictionary[js.Any]) = {
      val entityWithoutId = toScalaWithoutId(dict)
      val idOption = dict.get(ModelField.id[E].name).map(Scala2Js.toScala[Long])
      if (idOption.isDefined) {
        Entity.withId(idOption.get, entityWithoutId)
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
        ModelField.User.loginName,
        ModelField.User.passwordHash,
        ModelField.User.name,
        ModelField.User.isAdmin,
        ModelField.User.expandCashFlowTablesByDefault,
        ModelField.User.expandLiquidationTablesByDefault
      )

    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, User]) =
        getRequiredValueFromDict(dict)(field)

      User(
        loginName = getRequired(ModelField.User.loginName),
        passwordHash = getRequired(ModelField.User.passwordHash),
        name = getRequired(ModelField.User.name),
        isAdmin = getRequired(ModelField.User.isAdmin),
        expandCashFlowTablesByDefault = getRequired(ModelField.User.expandCashFlowTablesByDefault),
        expandLiquidationTablesByDefault = getRequired(ModelField.User.expandLiquidationTablesByDefault)
      )
    }
  }

  implicit object TransactionConverter extends EntityConverter[Transaction] {
    override def allFieldsWithoutId =
      Seq(
        ModelField.Transaction.transactionGroupId,
        ModelField.Transaction.issuerId,
        ModelField.Transaction.beneficiaryAccountCode,
        ModelField.Transaction.moneyReservoirCode,
        ModelField.Transaction.categoryCode,
        ModelField.Transaction.description,
        ModelField.Transaction.flowInCents,
        ModelField.Transaction.detailDescription,
        ModelField.Transaction.tags,
        ModelField.Transaction.createdDate,
        ModelField.Transaction.transactionDate,
        ModelField.Transaction.consumedDate
      )

    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, Transaction]) =
        getRequiredValueFromDict(dict)(field)

      Transaction(
        transactionGroupId = getRequired(ModelField.Transaction.transactionGroupId),
        issuerId = getRequired(ModelField.Transaction.issuerId),
        beneficiaryAccountCode = getRequired(ModelField.Transaction.beneficiaryAccountCode),
        moneyReservoirCode = getRequired(ModelField.Transaction.moneyReservoirCode),
        categoryCode = getRequired(ModelField.Transaction.categoryCode),
        description = getRequired(ModelField.Transaction.description),
        flowInCents = getRequired(ModelField.Transaction.flowInCents),
        detailDescription = getRequired(ModelField.Transaction.detailDescription),
        tags = getRequired(ModelField.Transaction.tags),
        createdDate = getRequired(ModelField.Transaction.createdDate),
        transactionDate = getRequired(ModelField.Transaction.transactionDate),
        consumedDate = getRequired(ModelField.Transaction.consumedDate)
      )
    }
  }

  implicit object TransactionGroupConverter extends EntityConverter[TransactionGroup] {
    override def allFieldsWithoutId = Seq(ModelField.TransactionGroup.createdDate)
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, TransactionGroup]) =
        getRequiredValueFromDict(dict)(field)

      TransactionGroup(createdDate = getRequired(ModelField.TransactionGroup.createdDate))
    }
  }

  implicit object BalanceCheckConverter extends EntityConverter[BalanceCheck] {
    override def allFieldsWithoutId =
      Seq(
        ModelField.BalanceCheck.issuerId,
        ModelField.BalanceCheck.moneyReservoirCode,
        ModelField.BalanceCheck.balanceInCents,
        ModelField.BalanceCheck.createdDate,
        ModelField.BalanceCheck.checkDate
      )
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, BalanceCheck]) =
        getRequiredValueFromDict(dict)(field)

      BalanceCheck(
        issuerId = getRequired(ModelField.BalanceCheck.issuerId),
        moneyReservoirCode = getRequired(ModelField.BalanceCheck.moneyReservoirCode),
        balanceInCents = getRequired(ModelField.BalanceCheck.balanceInCents),
        createdDate = getRequired(ModelField.BalanceCheck.createdDate),
        checkDate = getRequired(ModelField.BalanceCheck.checkDate)
      )
    }
  }

  implicit object ExchangeRateMeasurementConverter extends EntityConverter[ExchangeRateMeasurement] {
    override def allFieldsWithoutId =
      Seq(
        ModelField.ExchangeRateMeasurement.date,
        ModelField.ExchangeRateMeasurement.foreignCurrencyCode,
        ModelField.ExchangeRateMeasurement.ratioReferenceToForeignCurrency
      )
    override def toScalaWithoutId(dict: js.Dictionary[js.Any]) = {
      def getRequired[T](field: ModelField[T, ExchangeRateMeasurement]) =
        getRequiredValueFromDict(dict)(field)

      ExchangeRateMeasurement(
        date = getRequired(ModelField.ExchangeRateMeasurement.date),
        foreignCurrencyCode = getRequired(ModelField.ExchangeRateMeasurement.foreignCurrencyCode),
        ratioReferenceToForeignCurrency =
          getRequired(ModelField.ExchangeRateMeasurement.ratioReferenceToForeignCurrency)
      )
    }
  }
}
