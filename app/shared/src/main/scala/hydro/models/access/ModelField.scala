package hydro.models.access

import app.common.GuavaReplacement.ImmutableBiMap
import app.common.OrderToken
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.ModelField.FieldType

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

/**
  * Represents a field in an model entity.
  *
  * @param name A name for this field that is unique in E
  * @tparam V The type of the values
  * @tparam E The type corresponding to the entity that contains this field
  */
abstract class ModelField[V, E](val name: String, accessor: E => V)(implicit val fieldType: FieldType[V]) {

  def get(entity: E): V = accessor(entity)
}

object ModelField {

  def toBiMapWithUniqueValues(fields: ModelField[_, _]*): ImmutableBiMap[ModelField[_, _], Int] = {
    val resultBuilder = ImmutableBiMap.builder[ModelField[_, _], Int]()
    for ((field, index) <- fields.zipWithIndex) {
      resultBuilder.put(field, index + 1)
    }
    resultBuilder.build()
  }

  sealed trait FieldType[T]
  object FieldType {
    case class OptionType[V](fieldType: FieldType[V]) extends FieldType[Option[V]]
    implicit def optionType[V: FieldType]: FieldType[Option[V]] = OptionType(implicitly[FieldType[V]])

    implicit case object BooleanType extends FieldType[Boolean]
    implicit case object IntType extends FieldType[Int]
    implicit case object LongType extends FieldType[Long]
    implicit case object DoubleType extends FieldType[Double]
    implicit case object StringType extends FieldType[String]
    implicit case object LocalDateTimeType extends FieldType[LocalDateTime]
    implicit case object FiniteDurationType extends FieldType[FiniteDuration]
    implicit case object StringSeqType extends FieldType[Seq[String]]
    implicit case object OrderTokenType extends FieldType[OrderToken]
  }

  abstract class IdModelField[E <: Entity] extends ModelField[Long, E]("id", _.idOption getOrElse -1)
}
