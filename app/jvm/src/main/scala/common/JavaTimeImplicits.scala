package common

import java.time._

object JavaTimeImplicits {

  abstract class BaseWrapper[T <: Comparable[T]](thisComparable: T) {
    def <=(other: T): Boolean = (thisComparable compareTo other) <= 0
    def <(other: T): Boolean = (thisComparable compareTo other) < 0
    def >=(other: T): Boolean = (thisComparable compareTo other) >= 0
    def >(other: T): Boolean = (thisComparable compareTo other) > 0
    def ==(other: T): Boolean = (thisComparable compareTo other) == 0
  }

  abstract class BaseOrdering[T <: Comparable[T]] extends Ordering[T] {
    override def compare(x: T, y: T): Int = x compareTo y
  }

  implicit class InstantWrapper(instant: Instant) extends BaseWrapper[Instant](instant)
  implicit object InstantOrdering extends BaseOrdering[Instant]

  implicit class LocalDateWrapper(date: LocalDate) extends BaseWrapper[LocalDate](date)
  implicit object LocalDateOrdering extends BaseOrdering[LocalDate]
}
