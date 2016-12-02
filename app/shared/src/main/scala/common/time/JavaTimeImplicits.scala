package common.time

import java.time._
import java.time.chrono.ChronoLocalDate

object JavaTimeImplicits {

  abstract class BaseWrapper[T: Ordering](thisComparable: T)(implicit ordering: Ordering[T]) {
    def <=(other: T): Boolean = ordering.compare(thisComparable, other) <= 0
    def <(other: T): Boolean = ordering.compare(thisComparable, other) < 0
    def >=(other: T): Boolean = ordering.compare(thisComparable, other) >= 0
    def >(other: T): Boolean = ordering.compare(thisComparable, other) > 0
  }

  implicit object InstantOrdering extends Ordering[Instant] {
    override def compare(x: Instant, y: Instant): Int = x compareTo y
  }
  implicit class InstantWrapper(instant: Instant) extends BaseWrapper[Instant](instant)

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }
  implicit class LocalDateWrapper(date: LocalDate) extends BaseWrapper[LocalDate](date)
}
