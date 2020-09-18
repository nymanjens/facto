package hydro.api

import hydro.models.modification.EntityType
import hydro.api.PicklableDbQuery.Filter
import hydro.api.PicklableDbQuery.Sorting
import hydro.api.PicklableDbQuery.Sorting.FieldWithDirection
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.access.ModelField

import scala.collection.immutable.Seq

/** Fork of DbQuery that is picklable. */
case class PicklableDbQuery(
    filter: Filter,
    sorting: Option[Sorting],
    limit: Option[Int],
    entityType: EntityType.any,
) {
  def toRegular: DbQuery[_ <: Entity] = {
    def internal[E <: Entity] =
      DbQuery[E](
        filter = filter.toRegular.asInstanceOf[DbQuery.Filter[E]],
        sorting = sorting.map(_.toRegular.asInstanceOf[DbQuery.Sorting[E]]),
        limit = limit,
      )(entityType.asInstanceOf[EntityType[E]])
    internal
  }
}
object PicklableDbQuery {
  def fromRegular(regular: DbQuery[_]): PicklableDbQuery =
    PicklableDbQuery(
      filter = Filter.fromRegular(regular.filter),
      sorting = regular.sorting.map(Sorting.fromRegular),
      limit = regular.limit,
      entityType = regular.entityType.asInstanceOf[EntityType.any],
    )

  sealed trait Filter {
    def toRegular: DbQuery.Filter[_]
  }
  object Filter {
    def fromRegular(regular: DbQuery.Filter[_]): Filter = regular match {
      case DbQuery.Filter.NullFilter()           => NullFilter()
      case DbQuery.Filter.Equal(field, value)    => Equal(FieldWithValue.fromRegular(field, value))
      case DbQuery.Filter.NotEqual(field, value) => NotEqual(FieldWithValue.fromRegular(field, value))
      case f @ DbQuery.Filter.GreaterThan(field, value) =>
        GreaterThan(
          FieldWithValue.fromRegular(field, value),
          PicklableOrdering.fromRegular(f.picklableOrdering),
        )
      case f @ DbQuery.Filter.GreaterOrEqualThan(field, value) =>
        GreaterOrEqualThan(
          FieldWithValue.fromRegular(field, value),
          PicklableOrdering.fromRegular(f.picklableOrdering),
        )
      case f @ DbQuery.Filter.LessThan(field, value) =>
        LessThan(FieldWithValue.fromRegular(field, value), PicklableOrdering.fromRegular(f.picklableOrdering))
      case DbQuery.Filter.AnyOf(field, values) =>
        AnyOf(PicklableModelField.fromRegular(field), values.map(FieldWithValue.fromRegular(field, _)))
      case DbQuery.Filter.NoneOf(field, values) =>
        NoneOf(PicklableModelField.fromRegular(field), values.map(FieldWithValue.fromRegular(field, _)))
      case DbQuery.Filter.ContainsIgnoreCase(field, substring) =>
        ContainsIgnoreCase(PicklableModelField.fromRegular(field), substring)
      case DbQuery.Filter.DoesntContainIgnoreCase(field, substring) =>
        DoesntContainIgnoreCase(PicklableModelField.fromRegular(field), substring)
      case DbQuery.Filter.SeqContains(field, value) =>
        SeqContains(PicklableModelField.fromRegular(field), value)
      case DbQuery.Filter.SeqDoesntContain(field, value) =>
        SeqDoesntContain(PicklableModelField.fromRegular(field), value)
      case DbQuery.Filter.Or(filters)  => Or(filters.map(Filter.fromRegular))
      case DbQuery.Filter.And(filters) => And(filters.map(Filter.fromRegular))
    }

    case class NullFilter() extends Filter {
      override def toRegular = DbQuery.Filter.NullFilter()
    }
    case class Equal(fieldWithValue: FieldWithValue) extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter.Equal[V, E](
            fieldWithValue.field.toRegular.asInstanceOf[ModelField[V, E]],
            fieldWithValue.value.asInstanceOf[V],
          )
        internal
      }
    }
    case class NotEqual(fieldWithValue: FieldWithValue) extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter.NotEqual[V, E](
            fieldWithValue.field.toRegular.asInstanceOf[ModelField[V, E]],
            fieldWithValue.value.asInstanceOf[V],
          )
        internal
      }
    }
    case class GreaterThan(fieldWithValue: FieldWithValue, ordering: PicklableOrdering) extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter.GreaterThan[V, E](
            fieldWithValue.field.toRegular.asInstanceOf[ModelField[V, E]],
            fieldWithValue.value
              .asInstanceOf[V],
          )(ordering.toRegular.asInstanceOf[DbQuery.PicklableOrdering[V]])
        internal
      }
    }
    case class GreaterOrEqualThan(fieldWithValue: FieldWithValue, ordering: PicklableOrdering)
        extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter.GreaterOrEqualThan[V, E](
            fieldWithValue.field.toRegular.asInstanceOf[ModelField[V, E]],
            fieldWithValue.value
              .asInstanceOf[V],
          )(ordering.toRegular.asInstanceOf[DbQuery.PicklableOrdering[V]])
        internal
      }
    }
    case class LessThan(fieldWithValue: FieldWithValue, ordering: PicklableOrdering) extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter.LessThan[V, E](
            fieldWithValue.field.toRegular.asInstanceOf[ModelField[V, E]],
            fieldWithValue.value
              .asInstanceOf[V],
          )(ordering.toRegular.asInstanceOf[DbQuery.PicklableOrdering[V]])
        internal
      }
    }
    case class AnyOf(field: PicklableModelField, values: Seq[FieldWithValue]) extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter
            .AnyOf[V, E](field.toRegular.asInstanceOf[ModelField[V, E]], values.map(_.value.asInstanceOf[V]))
        internal
      }
    }
    case class NoneOf(field: PicklableModelField, values: Seq[FieldWithValue]) extends Filter {
      override def toRegular = {
        def internal[V, E]: DbQuery.Filter[_] =
          DbQuery.Filter
            .NoneOf[V, E](field.toRegular.asInstanceOf[ModelField[V, E]], values.map(_.value.asInstanceOf[V]))
        internal
      }
    }
    case class ContainsIgnoreCase(field: PicklableModelField, substring: String) extends Filter {
      override def toRegular = {
        def internal[E]: DbQuery.Filter[_] =
          DbQuery.Filter
            .ContainsIgnoreCase[E](field.toRegular.asInstanceOf[ModelField[String, E]], substring)
        internal
      }
    }
    case class DoesntContainIgnoreCase(field: PicklableModelField, substring: String) extends Filter {
      override def toRegular = {
        def internal[E]: DbQuery.Filter[_] =
          DbQuery.Filter
            .DoesntContainIgnoreCase[E](field.toRegular.asInstanceOf[ModelField[String, E]], substring)
        internal
      }
    }
    case class SeqContains(field: PicklableModelField, value: String) extends Filter {
      override def toRegular = {
        def internal[E]: DbQuery.Filter[_] =
          DbQuery.Filter.SeqContains[E](field.toRegular.asInstanceOf[ModelField[Seq[String], E]], value)
        internal
      }
    }
    case class SeqDoesntContain(field: PicklableModelField, value: String) extends Filter {
      override def toRegular = {
        def internal[E]: DbQuery.Filter[_] =
          DbQuery.Filter.SeqDoesntContain[E](field.toRegular.asInstanceOf[ModelField[Seq[String], E]], value)
        internal
      }
    }
    case class Or(filters: Seq[Filter]) extends Filter {
      override def toRegular = {
        def internal[E]: DbQuery.Filter[_] =
          DbQuery.Filter.Or[E](filters.map(_.toRegular.asInstanceOf[DbQuery.Filter[E]]))
        internal
      }
    }
    case class And(filters: Seq[Filter]) extends Filter {
      override def toRegular = {
        def internal[E]: DbQuery.Filter[_] =
          DbQuery.Filter.And[E](filters.map(_.toRegular.asInstanceOf[DbQuery.Filter[E]]))
        internal
      }
    }
  }

  case class Sorting(fieldsWithDirection: Seq[FieldWithDirection]) {
    def toRegular: DbQuery.Sorting[_] = {
      def internal[E] =
        DbQuery.Sorting[E](
          fieldsWithDirection.map(_.toRegular.asInstanceOf[DbQuery.Sorting.FieldWithDirection[_, E]])
        )
      internal
    }
  }
  object Sorting {
    def fromRegular(regular: DbQuery.Sorting[_]): Sorting =
      Sorting(regular.fieldsWithDirection.map(FieldWithDirection.fromRegular))

    case class FieldWithDirection(
        field: PicklableModelField,
        isDesc: Boolean,
        picklableValueOrdering: PicklableOrdering,
    ) {
      def toRegular: DbQuery.Sorting.FieldWithDirection[_, _] = {
        def internal[V] = {
          DbQuery.Sorting.FieldWithDirection(field.toRegular.asInstanceOf[ModelField[V, _]], isDesc)(
            picklableValueOrdering.toRegular.asInstanceOf[DbQuery.PicklableOrdering[V]]
          )
        }
        internal
      }
    }
    object FieldWithDirection {
      def fromRegular(regular: DbQuery.Sorting.FieldWithDirection[_, _]): FieldWithDirection =
        FieldWithDirection(
          PicklableModelField.fromRegular(regular.field),
          regular.isDesc,
          PicklableOrdering.fromRegular(regular.picklableValueOrdering),
        )
    }
  }

  sealed abstract class PicklableOrdering(val toRegular: DbQuery.PicklableOrdering[_])
  object PicklableOrdering {
    def fromRegular(regular: DbQuery.PicklableOrdering[_]): PicklableOrdering = regular match {
      case DbQuery.PicklableOrdering.LongOrdering          => LongOrdering
      case DbQuery.PicklableOrdering.MaybeLongOrdering     => MaybeLongOrdering
      case DbQuery.PicklableOrdering.IntOrdering           => IntOrdering
      case DbQuery.PicklableOrdering.MaybeIntOrdering      => MaybeIntOrdering
      case DbQuery.PicklableOrdering.StringOrdering        => StringOrdering
      case DbQuery.PicklableOrdering.LocalDateTimeOrdering => LocalDateTimeOrdering
    }

    case object LongOrdering extends PicklableOrdering(DbQuery.PicklableOrdering.LongOrdering)
    case object MaybeLongOrdering extends PicklableOrdering(DbQuery.PicklableOrdering.MaybeLongOrdering)
    case object IntOrdering extends PicklableOrdering(DbQuery.PicklableOrdering.IntOrdering)
    case object MaybeIntOrdering extends PicklableOrdering(DbQuery.PicklableOrdering.MaybeIntOrdering)
    case object StringOrdering extends PicklableOrdering(DbQuery.PicklableOrdering.StringOrdering)
    case object LocalDateTimeOrdering
        extends PicklableOrdering(DbQuery.PicklableOrdering.LocalDateTimeOrdering)
  }

  case class FieldWithValue(field: PicklableModelField, value: Any)
  object FieldWithValue {
    def fromRegular(field: ModelField.any, value: Any): FieldWithValue =
      FieldWithValue(PicklableModelField.fromRegular(field), value)
  }
}
