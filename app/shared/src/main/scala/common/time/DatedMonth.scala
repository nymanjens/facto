package common.time

import common.time.LocalDateTime
import scala.collection.immutable.Seq
import common.time.LocalDateTime
import java.time.{Month, LocalDate}

import common.I18n

case class DatedMonth(startDate: LocalDate) extends Ordered[DatedMonth] {
  TimeUtils.requireStartOfMonth(startDate)

  /** Returns abbreviation e.g. "Jan". */
  def abbreviation(implicit i18n: I18n): String = {
    val code = DatedMonth.abbreviationCodes(startDate.getMonth)
    i18n(code)
  }

  def contains(date: LocalDateTime): Boolean = {
    date.getYear == startDate.getYear && date.getMonth == startDate.getMonth
  }

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
