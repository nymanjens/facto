package common

import scala.collection.immutable.Seq

import com.google.common.collect.Range
import org.joda.time.{DateTime, Months}

import TimeUtils._

case class DatedMonth(startDate: DateTime) extends Ordered[DatedMonth] {
  TimeUtils.requireStartOfMonth(startDate)

  /** Returns abbreviation e.g. "Jan". */
  def abbreviation: String = {
    DatedMonth.abbreviations(startDate.getMonthOfYear - 1)
  }

  def contains(date: DateTime): Boolean = {
    date.getYear == startDate.getYear && date.getMonthOfYear == startDate.getMonthOfYear
  }

  override def compare(that: DatedMonth): Int = this.startDate compareTo that.startDate
}

object DatedMonth {

  private val abbreviations = Seq("Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sept", "Oct", "Nov", "Dec")

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
