package hydro.models.access

import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.common.OrderToken
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.ModelField.FieldType
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

/**
 * Represents a field in an model entity.
 *
 * @param name A name for this field that is unique in E
 * @tparam V The type of the values
 * @tparam E The type corresponding to the entity that contains this field
 */
case class ModelField[V, E] private (
    name: String,
    private val accessor: E => V,
    private val setter: V => E => E,
    entityType: EntityType.any,
    fieldType: FieldType[V],
) {
  def get(entity: E): V = accessor(entity)
  def getUnsafe(entity: Entity): V = get(entity.asInstanceOf[E])
  def set(entity: E, value: V): E = setter(value)(entity)
}

object ModelField {

  def apply[V, E <: Entity](
      name: String,
      accessor: E => V,
      setter: V => E => E,
  )(implicit
      fieldType: FieldType[V],
      entityType: EntityType[E],
  ): ModelField[V, E] = {
    new ModelField(
      name = name,
      accessor = accessor,
      setter = setter,
      entityType = entityType.asInstanceOf[EntityType.any],
      fieldType = fieldType,
    )
  }

  def forId[E <: Entity: EntityType](): ModelField[Long, E] = {
    implicit val fieldType: FieldType[Long] = FieldType.LongType
    ModelField(
      name = "id",
      accessor = _.idOption getOrElse -1,
      setter = v => _.withId(v).asInstanceOf[E],
    )
  }

  type any = ModelField[_, _]

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
}
