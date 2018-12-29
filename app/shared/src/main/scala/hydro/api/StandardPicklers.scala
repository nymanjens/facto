package hydro.api

import java.time.LocalDate
import java.time.LocalTime

import app.models.access.ModelField
import boopickle.Default._
import hydro.api.PicklableDbQuery.FieldWithValue
import hydro.common.time.LocalDateTime

abstract class StandardPicklers {

  implicit object LocalDateTimePickler extends Pickler[LocalDateTime] {
    override def pickle(dateTime: LocalDateTime)(implicit state: PickleState): Unit = logExceptions {
      val date = dateTime.toLocalDate
      val time = dateTime.toLocalTime

      state.pickle(date.getYear)
      state.pickle(date.getMonth.getValue)
      state.pickle(date.getDayOfMonth)
      state.pickle(time.getHour)
      state.pickle(time.getMinute)
      state.pickle(time.getSecond)
    }
    override def unpickle(implicit state: UnpickleState): LocalDateTime = logExceptions {
      LocalDateTime.of(
        LocalDate.of(
          state.unpickle[Int] /* year */,
          state.unpickle[Int] /* month */,
          state.unpickle[Int] /* dayOfMonth */
        ),
        LocalTime.of(
          state.unpickle[Int] /* hour */,
          state.unpickle[Int] /* minute */,
          state.unpickle[Int] /* second */
        )
      )
    }
  }

  implicit val fieldWithValuePickler: Pickler[FieldWithValue] =
    new Pickler[FieldWithValue] {
      override def pickle(obj: FieldWithValue)(implicit state: PickleState) = {
        def internal[E]: Unit = {
          state.pickle(obj.field)
          state.pickle(obj.value.asInstanceOf[E])(
            picklerForField(obj.field.toRegular).asInstanceOf[Pickler[E]])
        }
        internal
      }
      override def unpickle(implicit state: UnpickleState) = {
        def internal[E]: FieldWithValue = {
          val field = state.unpickle[PicklableModelField]
          val value = state.unpickle[E](picklerForField(field.toRegular).asInstanceOf[Pickler[E]])
          FieldWithValue(field = field, value = value)
        }
        internal
      }

      private def picklerForField(field: ModelField[_, _]): Pickler[_] = {
        def fromFieldType(fieldType: ModelField.FieldType[_]): Pickler[_] = {
          def fromType[V: Pickler](fieldType: ModelField.FieldType[V]): Pickler[V] = implicitly
          fieldType match {
            case ModelField.FieldType.OptionType(valueFieldType) =>
              optionPickler(fromFieldType(valueFieldType))
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
        }
        fromFieldType(field.fieldType)
      }
    }

  implicit val picklableDbQueryPickler: Pickler[PicklableDbQuery] = {
    implicit val fieldWithDirectionPickler: Pickler[PicklableDbQuery.Sorting.FieldWithDirection] =
      boopickle.Default.generatePickler
    implicit val sortingPickler: Pickler[PicklableDbQuery.Sorting] = boopickle.Default.generatePickler
    boopickle.Default.generatePickler
  }

  protected def logExceptions[T](codeBlock: => T): T = {
    try {
      codeBlock
    } catch {
      case t: Throwable =>
        println(s"  Caught exception while pickling: $t")
        t.printStackTrace()
        throw t
    }
  }
}
