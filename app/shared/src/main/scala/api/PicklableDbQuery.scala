package api

import api.PicklableDbQuery.Sorting.FieldWithDirection
import api.PicklableDbQuery.{Filter, Sorting}
import boopickle.Default._

import scala.collection.immutable.Seq

case class PicklableDbQuery(filter: Filter, sorting: Option[Sorting], limit: Option[Int])

object PicklableDbQuery {

  sealed trait Filter
  object Filter {
    case class NullFilter() extends Filter
    case class Equal(fieldWithValue: FieldWithValue) extends Filter
    case class NotEqual(fieldWithValue: FieldWithValue) extends Filter
    case class GreaterThan(fieldWithValue: FieldWithValue, ordering: PicklableOrdering) extends Filter
    case class GreaterOrEqualThan(fieldWithValue: FieldWithValue, ordering: PicklableOrdering) extends Filter
    case class LessThan(fieldWithValue: FieldWithValue, ordering: PicklableOrdering) extends Filter
    case class AnyOf(field: PicklableModelField, values: Seq[FieldWithValue]) extends Filter
    case class NoneOf(field: PicklableModelField, values: Seq[FieldWithValue]) extends Filter
    case class ContainsIgnoreCase(field: PicklableModelField, substring: String) extends Filter
    case class DoesntContainIgnoreCase(field: PicklableModelField, substring: String) extends Filter
    case class SeqContains(field: PicklableModelField, value: String) extends Filter
    case class SeqDoesntContain(field: PicklableModelField, value: String) extends Filter
    case class Or(filters: Seq[Filter]) extends Filter
    case class And(filters: Seq[Filter]) extends Filter
  }

  case class Sorting(fieldsWithDirection: Seq[FieldWithDirection])
  object Sorting {
    case class FieldWithDirection(field: PicklableModelField,
                                  isDesc: Boolean,
                                  picklableValueOrdering: PicklableOrdering)
  }

  sealed trait PicklableOrdering
  object PicklableOrdering {
    case object LongOrdering extends PicklableOrdering
    case object LocalDateTimeOrdering extends PicklableOrdering
  }

  case class FieldWithValue(field: PicklableModelField, value: Any)
}
