package common

import org.joda.time.{Months, DateTime}
import com.github.nscala_time.time.Imports._

import common.TimeUtils.{February, dateAt, January}

/**
  * Represents a continuous (possibly empty) range of dated months in time space.
  */
case class MonthRange(start: DateTime, startOfNextMonth: DateTime) {
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

  def countMonths: Int = Months.monthsBetween(start, startOfNextMonth).getMonths

  def contains(date: DateTime): Boolean = start <= date && date < startOfNextMonth

  def contains(month: DatedMonth): Boolean = contains(month.startDate)
}

object MonthRange {

  private val firstStartOfMonthSinceEpoch: DateTime = {
    // Needs to be february because we epoch may not have hours == 0 in the local time zone.
    dateAt(1970, February, 1)
  }

  private val lastPossibleStartOfMonth: DateTime = DatedMonth.containing(new DateTime(Long.MaxValue)).startDate

  val empty: MonthRange = MonthRange(firstStartOfMonthSinceEpoch, firstStartOfMonthSinceEpoch)

  def forYear(year: Int): MonthRange = {
    val startDate = dateAt(year, January, 1)
    val endDate = dateAt(year + 1, January, 1)
    MonthRange(startDate, endDate)
  }

  def atLeast(start: DateTime): MonthRange = MonthRange(start, lastPossibleStartOfMonth)
  def atLeast(datedMonth: DatedMonth): MonthRange = atLeast(datedMonth.startDate)

  def lessThan(startOfNextMonth: DateTime): MonthRange = MonthRange(firstStartOfMonthSinceEpoch, startOfNextMonth)
  def lessThan(datedMonth: DatedMonth): MonthRange = lessThan(datedMonth.startDate)

}
