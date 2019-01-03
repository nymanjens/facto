package hydro.scala2js

import java.time.LocalDate
import java.time.LocalTime

import app.common.GuavaReplacement.ImmutableBiMap
import app.common.OrderToken
import app.models.access.ModelFields
import app.models.modification._
import app.scala2js.AppConverters
import app.scala2js.AppConverters.EntityTypeConverter
import app.scala2js.AppConverters.fromEntityType
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.ModelField
import hydro.scala2js.Scala2Js.Converter
import hydro.scala2js.Scala2Js.MapConverter

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object StandardConverters {

  // **************** Convertor generators **************** //
  def fromModelField[V](modelField: ModelField[V, _]): Converter[V] = {
    def fromFieldType[V1](fieldType: ModelField.FieldType[V1]): Converter[V1] = {
      def fromType[V2: Converter](fieldType: ModelField.FieldType[V2]): Converter[V2] = implicitly
      val result = fieldType match {
        case ModelField.FieldType.OptionType(valueFieldType) =>
          optionConverter(fromFieldType(valueFieldType))
        case ModelField.FieldType.BooleanType        => fromType(ModelField.FieldType.BooleanType)
        case ModelField.FieldType.IntType            => fromType(ModelField.FieldType.IntType)
        case ModelField.FieldType.LongType           => fromType(ModelField.FieldType.LongType)
        case ModelField.FieldType.DoubleType         => fromType(ModelField.FieldType.DoubleType)
        case ModelField.FieldType.StringType         => fromType(ModelField.FieldType.StringType)
        case ModelField.FieldType.LocalDateTimeType  => fromType(ModelField.FieldType.LocalDateTimeType)
        case ModelField.FieldType.FiniteDurationType => fromType(ModelField.FieldType.FiniteDurationType)
        case ModelField.FieldType.StringSeqType      => fromType(ModelField.FieldType.StringSeqType)
        case ModelField.FieldType.OrderTokenType     => fromType(ModelField.FieldType.OrderTokenType)
      }
      result.asInstanceOf[Converter[V1]]
    }
    fromFieldType(modelField.fieldType)
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

  implicit def optionConverter[V: Converter]: Converter[Option[V]] =
    new Converter[Option[V]] {
      override def toJs(option: Option[V]) = option match {
        case Some(v) => Scala2Js.toJs(v)
        case None    => null
      }
      override def toScala(value: js.Any) = {
        if (value == null) {
          None
        } else {
          Some(Scala2Js.toScala[V](value))
        }
      }
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

  implicit object FiniteDurationConverter extends Converter[FiniteDuration] {

    private val secondsInDay = 60 * 60 * 24

    override def toJs(duration: FiniteDuration) = {
      Scala2Js.toJs(duration.toMillis)
    }
    override def toScala(value: js.Any) = {
      Scala2Js.toScala[Long](value).millis
    }
  }

  implicit object OrderTokenConverter extends Converter[OrderToken] {
    override def toJs(token: OrderToken) = {
      token.parts.toJSArray
    }
    override def toScala(value: js.Any) = {
      OrderToken(value.asInstanceOf[js.Array[Int]].toList)
    }
  }

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
                AppConverters.fromEntityType(modification.entityType.asInstanceOf[EntityType[E]])))
          case EntityModification.Update(entity) =>
            result.push(updateNumber)
            result.push(
              Scala2Js.toJs(entity.asInstanceOf[E])(
                AppConverters.fromEntityType(modification.entityType.asInstanceOf[EntityType[E]])))
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
  final class EntityConverter[E <: Entity: EntityType](allFieldsWithoutId: Seq[ModelField[_, E]] = Seq(),
                                                       toScalaWithoutId: EntityConverter.DictWrapper[E] => E)
      extends MapConverter[E] {
    override def toJs(entity: E) = {
      val result = js.Dictionary[js.Any]()

      def addField[V](field: ModelField[V, E]): Unit = {
        result.update(field.name, Scala2Js.toJs(field.get(entity), field))
      }
      for (field <- allFieldsWithoutId) {
        addField(field)
      }
      for (id <- entity.idOption) {
        result.update(ModelFields.id[E].name, Scala2Js.toJs(id, ModelFields.id[E]))
      }
      result
    }

    override def toScala(dict: js.Dictionary[js.Any]) = {
      val entityWithoutId = toScalaWithoutId(new EntityConverter.DictWrapper(dict))
      val idOption = dict.get(ModelFields.id[E].name).map(Scala2Js.toScala[Long])
      if (idOption.isDefined) {
        Entity.withId(idOption.get, entityWithoutId)
      } else {
        entityWithoutId
      }
    }
  }
  object EntityConverter {
    final class DictWrapper[E <: Entity: EntityType](val dict: js.Dictionary[js.Any]) {
      def getRequired[V](field: ModelField[V, E]): V = {
        require(dict.contains(field.name), s"Key ${field.name} is missing from ${js.JSON.stringify(dict)}")
        Scala2Js.toScala[V](dict(field.name))(fromModelField(field))
      }
    }
  }
}
