package common.time

import java.time.{LocalDate, LocalTime, Month}

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
}

object DatedMonth {

  private val abbreviationCodes: Map[Month, String] = Map(
    Month.JANUARY -> "facto.date.month.jan.abbrev",
    Month.FEBRUARY -> "facto.date.month.feb.abbrev",
    Month.MARCH -> "facto.date.month.mar.abbrev",
    Month.APRIL -> "facto.date.month.apr.abbrev",
    Month.MAY -> "facto.date.month.may.abbrev",
    Month.JUNE -> "facto.date.month.jun.abbrev",
    Month.JULY -> "facto.date.month.jul.abbrev",
    Month.AUGUST -> "facto.date.month.aug.abbrev",
    Month.SEPTEMBER -> "facto.date.month.sep.abbrev",
    Month.OCTOBER -> "facto.date.month.oct.abbrev",
    Month.NOVEMBER -> "facto.date.month.nov.abbrev",
    Month.DECEMBER -> "facto.date.month.dec.abbrev"
  )

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
