package hydro.models.access

import hydro.models.access.DbQuery.Filter
import hydro.models.access.DbQuery.PicklableOrdering

import scala.collection.immutable.Seq

object DbQueryImplicits {
  implicit class KeyWrapper[V, E](field: ModelField[V, E]) {
    def ===(value: V): Filter[E] = Filter.Equal(field, value)
    def !==(value: V): Filter[E] = Filter.NotEqual(field, value)
    def isAnyOf(values: Seq[V]): Filter[E] = Filter.AnyOf(field, values)
    def isNoneOf(values: Seq[V]): Filter[E] = Filter.NoneOf(field, values)
  }
  implicit class OrderedKeyWrapper[V: PicklableOrdering, E](field: ModelField[V, E]) {
    def <(value: V): Filter[E] = Filter.LessThan(field, value)
    def >(value: V): Filter[E] = Filter.GreaterThan(field, value)
    def >=(value: V): Filter[E] = Filter.GreaterOrEqualThan(field, value)
  }

  implicit class StringKeyWrapper[E](field: ModelField[String, E]) {
    def containsIgnoreCase(substring: String): Filter[E] = Filter.ContainsIgnoreCase(field, substring)
    def doesntContainIgnoreCase(substring: String): Filter[E] =
      Filter.DoesntContainIgnoreCase(field, substring)
  }

  implicit class SeqKeyWrapper[E](field: ModelField[Seq[String], E]) {
    def contains(value: String): Filter[E] = Filter.SeqContains(field, value)
    def doesntContain(value: String): Filter[E] = Filter.SeqDoesntContain(field, value)
  }

  implicit class FilterWrapper[E](thisFilter: Filter[E]) {
    def ||(otherFilter: Filter[E]): Filter[E] = Filter.Or(Seq(thisFilter, otherFilter))
    def &&(otherFilter: Filter[E]): Filter[E] = Filter.And(Seq(thisFilter, otherFilter))
  }
}
