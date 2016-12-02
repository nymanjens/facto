package common.time

import java.time.LocalDateTime
import java.time.{LocalDate, Period, ZoneId, Month}
import common.time.JavaTimeImplicits._

/**
  * Represents a continuous (possibly empty) range of dated months in time space.
  */
case class MonthRange(start: LocalDate, startOfNextMonth: LocalDate) {
  require(start <= startOfNextMonth, s"The start date ($start) should never be older than the start date of the next " +
    s"month ($startOfNextMonth). Use equal dates to represent an empty range.")
  TimeUtils.requireStartOfMonth(start)
  TimeUtils.requireStartOfMonth(startOfNextMonth)

  def completelyBefore(that: MonthRange): Boolean = this.startOfNextMonth <= that.start
  def completelyAfter(that: MonthRange): Boolean = this.start >= that.startOfNextMonth

  def intersection(that: MonthRange): MonthRange = (this, that) match {
    case _ if this completelyBefore that => MonthRange.empty
    case _ if this completelyAfter that => MonthRange.empty
    case _ =>
      val newStart = Set(this.start, that.start).max
      val newStartOfNextMonth = Set(this.startOfNextMonth, that.startOfNextMonth).min
      MonthRange(newStart, newStartOfNextMonth)
  }

  def countMonths: Int = Period.between(start, startOfNextMonth).toTotalMonths.toInt

  def contains(date: LocalDate): Boolean = start <= date && date < startOfNextMonth

  def contains(month: DatedMonth): Boolean = contains(month.startDate)

  def contains(dateTime: LocalDateTime): Boolean = contains(dateTime.toLocalDate)
}

object MonthRange {

  private val firstStartOfMonthSinceEpoch: LocalDate = {
    // Needs to be february because we epoch may not have hours == 0 in the local time zone.
    LocalDate.of(1970, Month.FEBRUARY, 1)
  }

  private val lastPossibleStartOfMonth: LocalDate = LocalDate.MAX.withDayOfMonth(1)

  val empty: MonthRange = MonthRange(firstStartOfMonthSinceEpoch, firstStartOfMonthSinceEpoch)

  def forYear(year: Int): MonthRange = {
    val startDate = LocalDate.of(year, Month.JANUARY, 1)
    val endDate = LocalDate.of(year + 1, Month.JANUARY, 1)
    MonthRange(startDate, endDate)
  }

  def atLeast(start: LocalDate): MonthRange = MonthRange(start, lastPossibleStartOfMonth)
  def atLeast(datedMonth: DatedMonth): MonthRange = atLeast(datedMonth.startDate)

  def lessThan(startOfNextMonth: LocalDate): MonthRange = MonthRange(firstStartOfMonthSinceEpoch, startOfNextMonth)
  def lessThan(datedMonth: DatedMonth): MonthRange = lessThan(datedMonth.startDate)

}
