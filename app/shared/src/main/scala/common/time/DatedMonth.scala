package common.time

import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

import common.I18n

import scala.collection.immutable.Seq

case class DatedMonth(startDate: LocalDate) extends Ordered[DatedMonth] {
  TimeUtils.requireStartOfMonth(startDate)

  /** Returns abbreviation e.g. "Jan". */
  def abbreviation(implicit i18n: I18n): String = {
    val code = DatedMonth.abbreviationCodes(startDate.getMonth)
    i18n(code)
  }

  def month: Month = startDate.getMonth
  def year: Int = startDate.getYear

  def contains(date: LocalDateTime): Boolean = {
    date.getYear == startDate.getYear && date.getMonth == startDate.getMonth
  }

  def startDateOfNextMonth: LocalDate = {
    val result = startDate.plusMonths(1)
    TimeUtils.requireStartOfMonth(result)
    result
  }

  def startTime: LocalDateTime = LocalDateTime.of(startDate, LocalTime.MIN)
  def startTimeOfNextMonth: LocalDateTime = LocalDateTime.of(startDateOfNextMonth, LocalTime.MIN)

  override def compare(that: DatedMonth): Int = this.startDate compareTo that.startDate
  override def toString = s"$month $year"
}

object DatedMonth {

  private val abbreviationCodes: Map[Month, String] = Map(
    Month.JANUARY -> "app.date.month.jan.abbrev",
    Month.FEBRUARY -> "app.date.month.feb.abbrev",
    Month.MARCH -> "app.date.month.mar.abbrev",
    Month.APRIL -> "app.date.month.apr.abbrev",
    Month.MAY -> "app.date.month.may.abbrev",
    Month.JUNE -> "app.date.month.jun.abbrev",
    Month.JULY -> "app.date.month.jul.abbrev",
    Month.AUGUST -> "app.date.month.aug.abbrev",
    Month.SEPTEMBER -> "app.date.month.sep.abbrev",
    Month.OCTOBER -> "app.date.month.oct.abbrev",
    Month.NOVEMBER -> "app.date.month.nov.abbrev",
    Month.DECEMBER -> "app.date.month.dec.abbrev"
  )

  def of(year: Int, month: Month): DatedMonth = DatedMonth(LocalDate.of(year, month, 1))

  def containing(date: LocalDate): DatedMonth = {
    DatedMonth(startOfMonthContaining(date))
  }

  def containing(dateTime: LocalDateTime): DatedMonth = containing(dateTime.toLocalDate)

  def allMonthsIn(year: Int): Seq[DatedMonth] = {
    Month.values()
    for (month <- TimeUtils.allMonths)
      yield DatedMonth(LocalDate.of(year, month, 1))
  }

  private def startOfMonthContaining(date: LocalDate): LocalDate = {
    date.withDayOfMonth(1)
  }
}
