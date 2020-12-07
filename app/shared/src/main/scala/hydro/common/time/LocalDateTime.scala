package hydro.common.time

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

import hydro.common.Require.requireNonNull

/**
 * Drop-in replacement for java.time.LocalDateTime, which isn't supported by scala.js yet.
 */
trait LocalDateTime extends Comparable[LocalDateTime] {

  def toLocalDate: LocalDate
  def toLocalTime: LocalTime
  def getYear: Int
  def getMonth: Month

  def plus(duration: Duration): LocalDateTime
  def minus(duration: Duration): LocalDateTime
}

object LocalDateTime {

  val MIN: LocalDateTime = LocalDateTime.of(LocalDate.MIN, LocalTime.MIN)
  val MAX: LocalDateTime = LocalDateTime.of(LocalDate.MAX, LocalTime.MAX)

  def of(localDate: LocalDate, localTime: LocalTime): LocalDateTime = {
    if (localTime.getNano != 0) {
      // Reduce precision to second precision
      LocalDateTimeImpl(localDate, LocalTime.of(localTime.getHour, localTime.getMinute, localTime.getSecond))
    } else {
      LocalDateTimeImpl(localDate, localTime)
    }
  }

  def of(year: Int, month: Month, dayOfMonth: Int, hour: Int, minute: Int): LocalDateTime = {
    val date: LocalDate = LocalDate.of(year, month, dayOfMonth)
    val time: LocalTime = LocalTime.of(hour, minute)
    LocalDateTime.of(date, time)
  }

  private case class LocalDateTimeImpl(private val date: LocalDate, private val time: LocalTime)
      extends LocalDateTime {
    requireNonNull(date, time)

    override def toLocalDate = date
    override def toLocalTime = time
    override def getYear = date.getYear
    override def getMonth = date.getMonth

    override def plus(duration: Duration) = {
      val millisInDay = 1000 * 60 * 60 * 24
      val durationMillis = duration.toMillis
      val timeMillis = time.toNanoOfDay / 1000 / 1000
      val dateMillis = date.toEpochDay * millisInDay

      val newdateMillis = dateMillis + timeMillis + durationMillis
      of(LocalDate.ofEpochDay(newdateMillis / millisInDay), time plus duration)
    }
    override def minus(duration: Duration) = plus(duration.negated())

    override def toString = s"$date $time"

    override def compareTo(other: LocalDateTime): Int = {
      var cmp = this.date.compareTo(other.toLocalDate)
      if (cmp == 0) cmp = this.time.compareTo(other.toLocalTime)
      cmp
    }
  }
}
