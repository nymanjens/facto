package common

import java.time._

object JavaTimeImplicits {

  implicit class LocalDateWrapper(thisDate: LocalDate) {
    def <=(other: LocalDate): Boolean = {
      val result = thisDate compareTo other
      result <= 0
    }
    def <(other: LocalDate): Boolean = {
      val result = thisDate compareTo other
      result < 0
    }
    def >=(other: LocalDate): Boolean = {
      val result = thisDate compareTo other
      result >= 0
    }
    def >(other: LocalDate): Boolean = {
      val result = thisDate compareTo other
      result > 0
    }
    def ==(other: LocalDate): Boolean = {
      val result = thisDate compareTo other
      result == 0
    }
  }

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }
}
