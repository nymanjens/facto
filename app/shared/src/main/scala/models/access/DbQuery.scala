package models.access

import models.access.DbQuery.Sorting.FieldWithDirection
import models.access.DbQuery.{Filter, Operation, Sorting}

import scala.collection.immutable.Seq

case class DbQuery[E, ReturnT](filter: Filter[E],
                               sorting: Sorting[E],
                               limit: Int = Int.MaxValue,
                               operation: Operation[E, ReturnT])

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
    case class GreaterThan[V, E](field: ModelField[V, E], value: V) extends Filter[E] {
//      override def apply(entity: E) = field.get(entity) > value
      override def apply(entity: E) = ???
    }
    case class GreaterOrEqualThan[V, E](field: ModelField[V, E], value: V) extends Filter[E] {
//      override def apply(entity: E) = field.get(entity) >= value
      override def apply(entity: E) = ???
    }
    case class LessThan[V, E](field: ModelField[V, E], value: V) extends Filter[E] {
//      override def apply(entity: E) = field.get(entity) < value
      override def apply(entity: E) = ???
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
    case class SeqContains[V, E](field: ModelField[Seq[V], E], value: V) extends Filter[E] {
      override def apply(entity: E) = field.get(entity) contains value
    }
    case class SeqDoesntContain[V, E](field: ModelField[Seq[V], E], value: V) extends Filter[E] {
      override def apply(entity: E) = !(field.get(entity) contains value)
    }
    case class Or[E](filters: Seq[Filter[E]]) extends Filter[E] {
      override def apply(entity: E) = filters.toStream.exists(_.apply(entity))
    }
    case class And[E](filters: Seq[Filter[E]]) extends Filter[E] {
      override def apply(entity: E) = filters.toStream.forall(_.apply(entity))
    }
  }

  case class Sorting[E] private (private[DbQuery] val fieldsWithDirection: Seq[FieldWithDirection[E]]) {
    def thenAscBy[V: Ordering](field: ModelField[V, E]): Sorting[E] = thenBy(field, isDesc = false)
    def thenDescBy[V: Ordering](field: ModelField[V, E]): Sorting[E] = thenBy(field, isDesc = true)
    def thenBy[V: Ordering](field: ModelField[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(fieldsWithDirection :+ FieldWithDirection(field, isDesc = isDesc))
  }
  object Sorting {
    def ascBy[V: Ordering, E](field: ModelField[V, E]): Sorting[E] = by(field, isDesc = false)
    def descBy[V: Ordering, E](field: ModelField[V, E]): Sorting[E] = by(field, isDesc = true)
    def by[V: Ordering, E](field: ModelField[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(Seq(FieldWithDirection(field, isDesc = isDesc)))

    private[DbQuery] case class FieldWithDirection[E](field: ModelField[_, E], isDesc: Boolean)
  }

  sealed trait Operation[E, ReturnT]
  object Operation {
    final class GetDataSeq[E] extends Operation[E, Seq[E]]
    final class Count[E]() extends Operation[E, Int]
  }
}
