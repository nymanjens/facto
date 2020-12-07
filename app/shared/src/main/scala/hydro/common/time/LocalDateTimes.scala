package hydro.common.time

import java.time.LocalTime
import java.time.Month

/**
 * Extension of `LocalDateTime`, which should keep the same API as `java.time.LocalDateTime`.
 */
object LocalDateTimes {

  def ofJavaLocalDateTime(javaDateTime: java.time.LocalDateTime): LocalDateTime = {
    LocalDateTime.of(javaDateTime.toLocalDate, javaDateTime.toLocalTime)
  }

  def createDateTime(year: Int, month: Month, dayOfMonth: Int): LocalDateTime = {
    LocalDateTime.of(year, month, dayOfMonth, 0, 0)
  }

  def toStartOfDay(localDateTime: LocalDateTime): LocalDateTime = {
    LocalDateTime.of(localDateTime.toLocalDate, LocalTime.MIN)
  }
}
