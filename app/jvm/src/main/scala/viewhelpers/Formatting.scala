package viewhelpers

import java.lang.Math.abs

import java.time.{Instant, LocalDate, Month, ZoneId}
import java.time.format.DateTimeFormatter

import play.api.i18n.Messages

import common.Clock

object Formatting {

  def formatDate(instant: Instant)
                (implicit messages: Messages) = {
    val zone = ZoneId.of("Europe/Paris")
    val now = Clock.now.atZone(zone).toLocalDate
    val date = instant.atZone(zone).toLocalDate

    val yearString = DateTimeFormatter.ofPattern("yy").format(date)
    val dayMonthString = DateTimeFormatter.ofPattern("d").format(date) + " " + extractMonth(date)
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

  def formatDateTime(instant: Instant) = {
    val zone = ZoneId.of("Europe/Paris")
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").format(instant.atZone(zone).toLocalDate)
  }

  private def extractDayOfWeek(date: LocalDate)
                      (implicit messages: Messages): String = {
    val dayAbbrevEnglish = DateTimeFormatter.ofPattern("EEE").format(date).toLowerCase
    Messages(s"facto.date.dayofweek.$dayAbbrevEnglish.abbrev")
  }

  private def extractMonth(date: LocalDate)
                  (implicit messages: Messages): String = {
    val monthAbbrevEnglish = DateTimeFormatter.ofPattern("MMM").format(date).toLowerCase
    Messages(s"facto.date.month.$monthAbbrevEnglish.abbrev")
  }
}
