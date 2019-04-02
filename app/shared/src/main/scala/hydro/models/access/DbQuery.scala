package hydro.models.access

import hydro.models.modification.EntityType
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.DbQuery.Filter
import hydro.models.access.DbQuery.Sorting
import hydro.models.access.DbQuery.Sorting.FieldWithDirection

import scala.collection.immutable.Seq
import scala.math.Ordering.Implicits._

/** Represents a simple database query in a portable way. */
case class DbQuery[E <: Entity](filter: Filter[E], sorting: Option[Sorting[E]], limit: Option[Int])(
    implicit val entityType: EntityType[E])

object DbQuery {

  sealed trait PicklableOrdering[T] {
    def toOrdering: Ordering[T]
  }
  object PicklableOrdering {
    implicit case object LongOrdering extends PicklableOrdering[Long] {
      override def toOrdering: Ordering[Long] = implicitly[Ordering[Long]]
    }
    implicit case object MaybeLongOrdering extends PicklableOrdering[Option[Long]] {
      override def toOrdering: Ordering[Option[Long]] = implicitly[Ordering[Option[Long]]]
    }
    implicit case object IntOrdering extends PicklableOrdering[Int] {
      override def toOrdering: Ordering[Int] = implicitly[Ordering[Int]]
    }
    implicit case object MaybeIntOrdering extends PicklableOrdering[Option[Int]] {
      override def toOrdering: Ordering[Option[Int]] = implicitly[Ordering[Option[Int]]]
    }
    implicit case object StringOrdering extends PicklableOrdering[String] {
      override def toOrdering: Ordering[String] = implicitly[Ordering[String]]
    }
    implicit case object LocalDateTimeOrdering extends PicklableOrdering[LocalDateTime] {
      override def toOrdering: Ordering[LocalDateTime] = implicitly[Ordering[LocalDateTime]]
    }
  }

  sealed trait Filter[E] {
    def apply(entity: E): Boolean
  }
  object Filter {
    case class NullFilter[E]() extends Filter[E] {
      override def apply(entity: E) = true
    }
    case class Equal[V, E](field: ModelField[V, E], value: V) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) == value
    }
    case class NotEqual[V, E](field: ModelField[V, E], value: V) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) != value
    }
    case class GreaterThan[V, E](field: ModelField[V, E], value: V)(
        implicit val picklableOrdering: PicklableOrdering[V])
        extends Filter[E] {
      override def apply(entity: E) = {
        implicit val ordering = implicitly[PicklableOrdering[V]].toOrdering
        field.get(entity) > value
      }
    }
    case class GreaterOrEqualThan[V, E](field: ModelField[V, E], value: V)(
        implicit val picklableOrdering: PicklableOrdering[V])
        extends Filter[E] {
      override def apply(entity: E) = {
        implicit val ordering = implicitly[PicklableOrdering[V]].toOrdering
        field.get(entity) >= value
      }
    }
    case class LessThan[V, E](field: ModelField[V, E], value: V)(
        implicit val picklableOrdering: PicklableOrdering[V])
        extends Filter[E] {
      override def apply(entity: E) = {
        implicit val ordering = implicitly[PicklableOrdering[V]].toOrdering
        field.get(entity) < value
      }
    }
    case class AnyOf[V, E](field: ModelField[V, E], values: Seq[V]) extends Filter[E] {
      override def apply(entity: E) = values contains field.get(entity)
    }
    case class NoneOf[V, E](field: ModelField[V, E], values: Seq[V]) extends Filter[E] {
      override def apply(entity: E) = !(values contains field.get(entity))
    }
    case class ContainsIgnoreCase[E](field: ModelField[String, E], substring: String) extends Filter[E] {
      private val substringLower = substring.toLowerCase
      override def apply(entity: E) = field.get(entity).toLowerCase contains substringLower
    }
    case class DoesntContainIgnoreCase[E](field: ModelField[String, E], substring: String) extends Filter[E] {
      private val substringLower = substring.toLowerCase
      override def apply(entity: E) = !(field.get(entity).toLowerCase contains substringLower)
    }
    case class SeqContains[E](field: ModelField[Seq[String], E], value: String) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) contains value
    }
    case class SeqDoesntContain[E](field: ModelField[Seq[String], E], value: String) extends Filter[E] {
      override def apply(entity: E) = !(field.get(entity) contains value)
    }
    case class Or[E](filters: Seq[Filter[E]]) extends Filter[E] {
      override def apply(entity: E) = filters.toStream.exists(_.apply(entity))
    }
    case class And[E](filters: Seq[Filter[E]]) extends Filter[E] {
      override def apply(entity: E) = filters.toStream.forall(_.apply(entity))
    }
  }

  case class Sorting[E] private (fieldsWithDirection: Seq[FieldWithDirection[_, E]]) {
    def thenAscBy[V: PicklableOrdering](field: ModelField[V, E]): Sorting[E] = thenBy(field, isDesc = false)
    def thenDescBy[V: PicklableOrdering](field: ModelField[V, E]): Sorting[E] = thenBy(field, isDesc = true)
    def thenBy[V: PicklableOrdering](field: ModelField[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(fieldsWithDirection :+ FieldWithDirection[V, E](field, isDesc = isDesc))

    def toOrdering: Ordering[E] = (x: E, y: E) => {
      fieldsWithDirection.toStream.flatMap {
        case f @ DbQuery.Sorting.FieldWithDirection(field, _) =>
          f.ordering.compare(field.get(x), field.get(y)) match {
            case 0      => None
            case result => Some(result)
          }
      }.headOption getOrElse 0
    }

    def reversed: Sorting[E] =
      Sorting(fieldsWithDirection.map(field => {
        def internal[V](field: FieldWithDirection[V, E]): FieldWithDirection[V, E] = {
          field.copy(isDesc = !field.isDesc)(field.picklableValueOrdering)
        }
        internal(field)
      }))
  }
  object Sorting {
    def ascBy[V: PicklableOrdering, E](field: ModelField[V, E]): Sorting[E] = by(field, isDesc = false)
    def descBy[V: PicklableOrdering, E](field: ModelField[V, E]): Sorting[E] = by(field, isDesc = true)
    def by[V: PicklableOrdering, E](field: ModelField[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(Seq(FieldWithDirection(field, isDesc = isDesc)))

    case class FieldWithDirection[V, E](field: ModelField[V, E], isDesc: Boolean)(
        implicit val picklableValueOrdering: PicklableOrdering[V]) {
      def ordering: Ordering[V] = {
        val ascendingOrdering = picklableValueOrdering.toOrdering
        if (isDesc) ascendingOrdering.reverse else ascendingOrdering
      }
    }
  }
}
