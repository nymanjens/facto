package common

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import com.google.common.collect.Range
import java.time.{Instant, ZoneId, Month, LocalDate}
import TimeUtils._
import play.api.i18n.Messages
import TimeUtils._

case class DatedMonth(startDate: LocalDate) extends Ordered[DatedMonth] {
  TimeUtils.requireStartOfMonth(startDate)

  /** Returns abbreviation e.g. "Jan". */
  def abbreviation(implicit messages: Messages): String = {
    val code = DatedMonth.abbreviationCodes(startDate.getMonth)
    Messages(code)
  }

  def contains(instant: Instant): Boolean = {
    val zone = ZoneId.of("Europe/Paris")
    val date = instant.atZone(zone)
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

  def containing(instant: Instant): DatedMonth = {
    val zone = ZoneId.of("Europe/Paris")
    val date = instant.atZone(zone).toLocalDate
    DatedMonth(startOfMonthContaining(date))
  }

  def allMonthsIn(year: Int): Seq[DatedMonth] = {
    Month.values()
    for (month <- TimeUtils.allMonths)
      yield DatedMonth(LocalDate.of(year, month, 1))
  }

  private def startOfMonthContaining(date: LocalDate): LocalDate = {
    date.withDayOfMonth(1)
  }
}
