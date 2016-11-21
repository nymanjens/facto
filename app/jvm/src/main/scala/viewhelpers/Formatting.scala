package viewhelpers

import java.lang.Math.abs

import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat.forPattern
import com.github.nscala_time.time.Imports._
import play.api.i18n.Messages

import common.Clock

object Formatting {

  def formatDate(date: Instant)
                (implicit messages: Messages) = {
    val now = Clock.now

    val yearString = date.toString(forPattern("yy"))
    val dayMonthString = date.toString(forPattern("d")) + " " + extractMonth(date)
    val dayOfWeek = extractDayOfWeek(date)

    if (date.getYear == now.getYear) {
      val dayDifference = abs(now.getDayOfYear - date.getDayOfYear)

      if (date.getDayOfYear == now.getDayOfYear) {
        Messages("facto.today")
      } else if (date.getDayOfYear == now.getDayOfYear - 1) {
        Messages("facto.yesterday")
      } else if (date.getDayOfYear == now.getDayOfYear + 1) {
        Messages("facto.tomorrow")
      } else if (dayDifference < 7) {
        s"$dayOfWeek, $dayMonthString"
      } else {
        dayMonthString
      }
    } else {
      s"$dayMonthString '$yearString"
    }
  }

  def formatDateTime(date: Instant) = {
    date.toString(forPattern("d MMM yyyy, HH:mm"))
  }

  def extractDayOfWeek(date: Instant)
                      (implicit messages: Messages): String = {
    val dayAbbrevEnglish = date.toString(forPattern("EEE")).toLowerCase
    Messages(s"facto.date.dayofweek.$dayAbbrevEnglish.abbrev")
  }

  def extractMonth(date: Instant)
                  (implicit messages: Messages): String = {
    val monthAbbrevEnglish = date.toString(forPattern("MMM")).toLowerCase
    Messages(s"facto.date.month.$monthAbbrevEnglish.abbrev")
  }
}
