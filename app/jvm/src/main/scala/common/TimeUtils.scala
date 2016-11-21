package common

import scala.collection.{Seq => MutableSeq}
import scala.collection.immutable.Seq
import com.google.common.collect.Range
import org.joda.time.{Instant, Months}
import play.api.data.{FormError, Forms}

object TimeUtils {

  def dateAt(year: Int, month: Month, dayOfMonth: Int): Instant = {
    new Instant(
      year,
      month.number,
      dayOfMonth,
      0 /* hourOfDay */ ,
      0 /* minuteOfHour */
    )
  }

  def requireStartOfMonth(date: Instant): Unit = {
    require(date.getDayOfMonth == 1, s"Date $date should be at the first day of the month.")
    require(date.getMillisOfDay == 0, s"Date $date should be at the first millisecond of the day, but millisOfDay was ${date.getMillisOfDay}.")
  }

  sealed abstract class Month(val number: Int) {
    require(number > 0)

    def index: Int = number - 1
  }

  def allMonths: Seq[Month] = Seq(January, February, March, April, May, June, July, August, September, October, November, December)

  object January extends Month(1)
  object February extends Month(2)
  object March extends Month(3)
  object April extends Month(4)
  object May extends Month(5)
  object June extends Month(6)
  object July extends Month(7)
  object August extends Month(8)
  object September extends Month(9)
  object October extends Month(10)
  object November extends Month(11)
  object December extends Month(12)


  /**
    * Parses the incoming date string to a Instant.
    *
    * @param dateString in the form of yyyy-mm-dd, e.g. "2016-03-13".
    * @throws IllegalArgumentException if the given string could not be parsed
    */
  def parseDateString(dateString: String): Instant = {
    val parsedDate: Either[MutableSeq[FormError], Instant] = Forms.jodaDate("yyyy-MM-dd").bind(Map("" -> dateString))
    parsedDate match {
      case Left(error) => throw new IllegalArgumentException(error.toString)
      case Right(date) => date
    }
  }
}
