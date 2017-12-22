package models.access

import models.access.DbQuery.Sorting.FieldWithDirection
import models.access.DbQuery.{Filter, Sorting}
import scala.math.Ordering.Implicits._

import scala.collection.immutable.Seq

case class DbQuery[E](filter: Filter[E], sorting: Option[Sorting[E]], limit: Option[Int])

object DbQuery {

  trait Filter[E] {
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
    case class GreaterThan[V: Ordering, E](field: ModelField[V, E], value: V) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) > value
    }
    case class GreaterOrEqualThan[V: Ordering, E](field: ModelField[V, E], value: V) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) >= value
    }
    case class LessThan[V: Ordering, E](field: ModelField[V, E], value: V) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) < value
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
    case class DoesntContainIgnoreCase[E](field: ModelField[String, E], substring: String)
        extends Filter[E] {
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
    def thenAscBy[V: Ordering](field: ModelField[V, E]): Sorting[E] = thenBy(field, isDesc = false)
    def thenDescBy[V: Ordering](field: ModelField[V, E]): Sorting[E] = thenBy(field, isDesc = true)
    def thenBy[V: Ordering](field: ModelField[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(fieldsWithDirection :+ FieldWithDirection[V, E](field, isDesc = isDesc))
  }
  object Sorting {
    def ascBy[V: Ordering, E](field: ModelField[V, E]): Sorting[E] = by(field, isDesc = false)
    def descBy[V: Ordering, E](field: ModelField[V, E]): Sorting[E] = by(field, isDesc = true)
    def by[V: Ordering, E](field: ModelField[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(Seq(FieldWithDirection(field, isDesc = isDesc)))

    case class FieldWithDirection[V: Ordering, E](field: ModelField[V, E], isDesc: Boolean) {
      def valueOrdering: Ordering[V] = implicitly[Ordering[V]]
    }
  }
}
