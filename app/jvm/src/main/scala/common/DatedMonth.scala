package common

import scala.collection.immutable.Seq
import com.google.common.collect.Range
import org.joda.time.{DateTime, Months}
import TimeUtils._
import play.api.i18n.Messages

case class DatedMonth(startDate: DateTime) extends Ordered[DatedMonth] {
  TimeUtils.requireStartOfMonth(startDate)

  /** Returns abbreviation e.g. "Jan". */
  def abbreviation(implicit messages: Messages): String = {
    val code = DatedMonth.abbreviationCodes(startDate.getMonthOfYear - 1)
    Messages(code)
  }

  def contains(date: DateTime): Boolean = {
    date.getYear == startDate.getYear && date.getMonthOfYear == startDate.getMonthOfYear
  }

  override def compare(that: DatedMonth): Int = this.startDate compareTo that.startDate
}

object DatedMonth {

  private val abbreviationCodes = Seq(
    "facto.date.month.jan.abbrev",
    "facto.date.month.feb.abbrev",
    "facto.date.month.mar.abbrev",
    "facto.date.month.apr.abbrev",
    "facto.date.month.may.abbrev",
    "facto.date.month.jun.abbrev",
    "facto.date.month.jul.abbrev",
    "facto.date.month.aug.abbrev",
    "facto.date.month.sep.abbrev",
    "facto.date.month.oct.abbrev",
    "facto.date.month.nov.abbrev",
    "facto.date.month.dec.abbrev"
  )

  def containing(date: DateTime): DatedMonth = DatedMonth(startOfMonthContaining(date))

  def allMonthsIn(year: Int): Seq[DatedMonth] = {
    for (month <- TimeUtils.allMonths)
      yield DatedMonth(dateAt(year, month, 1))
  }

  private def startOfMonthContaining(date: DateTime): DateTime = {
    date
      .withDayOfMonth(1)
      .withMillisOfDay(0)
  }
}
