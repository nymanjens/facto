package common.time

import java.time.{LocalDate, LocalTime, Month}
import common.Require.requireNonNull

/**
  * Drop-in replacement for java.time.LocalDateTime, which isn't supported by scala.js yet.
  */
trait LocalDateTime extends Comparable[LocalDateTime] {

  def toLocalDate: LocalDate
  def toLocalTime: LocalTime
  def getYear: Int
  def getMonth: Month
}

object LocalDateTime {

  val MIN: LocalDateTime = LocalDateTime.of(LocalDate.MIN, LocalTime.MIN);
  val MAX: LocalDateTime = LocalDateTime.of(LocalDate.MAX, LocalTime.MAX);

  def of(localDate: LocalDate, localTime: LocalTime): LocalDateTime = LocalDateTimeImpl(localDate, localTime)

  def ofJavaLocalDateTime(javaDateTime: java.time.LocalDateTime): LocalDateTime = {
    LocalDateTime.of(javaDateTime.toLocalDate, javaDateTime.toLocalTime)
  }

  private case class LocalDateTimeImpl(private val localDate: LocalDate,
                                       private val localTime: LocalTime) extends LocalDateTime {
    requireNonNull(localDate, localTime)

    override def toLocalDate = localDate
    override def toLocalTime = localTime
    override def getYear = localDate.getYear
    override def getMonth = localDate.getMonth

    override def compareTo(other: LocalDateTime): Int = {
      var cmp = this.localDate.compareTo(other.toLocalDate)
      if (cmp == 0) cmp = this.localTime.compareTo(other.toLocalTime)
      cmp
    }
  }
}