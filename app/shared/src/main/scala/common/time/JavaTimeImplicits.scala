package common.time

import java.time.LocalDate

object JavaTimeImplicits {

  abstract class BaseWrapper[T: Ordering](thisComparable: T)(implicit ordering: Ordering[T]) {
    def <=(other: T): Boolean = ordering.compare(thisComparable, other) <= 0
    def <(other: T): Boolean = ordering.compare(thisComparable, other) < 0
    def >=(other: T): Boolean = ordering.compare(thisComparable, other) >= 0
    def >(other: T): Boolean = ordering.compare(thisComparable, other) > 0
  }

  implicit object LocalDateTimeOrdering extends Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int = x compareTo y
  }
  implicit class LocalDateTimeWrapper(LocalDateTime: LocalDateTime) extends BaseWrapper[LocalDateTime](LocalDateTime)

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }
  implicit class LocalDateWrapper(date: LocalDate) extends BaseWrapper[LocalDate](date)
}
