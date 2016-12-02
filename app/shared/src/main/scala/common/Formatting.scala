package common

import java.lang.Math.abs
import java.time.format.DateTimeFormatter
import common.time.LocalDateTime
import java.time.{LocalDate, ZoneId}

import common.time.Clock

object Formatting {

  def formatDate(dateTime: LocalDateTime)
                (implicit i18n: I18n, clock: Clock): String = {
    val zone = ZoneId.of("Europe/Paris")
    val now = clock.now.toLocalDate
    val date = dateTime.toLocalDate

    val yearString = DateTimeFormatter.ofPattern("yy").format(date)
    val dayMonthString = DateTimeFormatter.ofPattern("d").format(date) + " " + extractMonth(date)
    val dayOfWeek = extractDayOfWeek(date)

    if (date.getYear == now.getYear) {
      val dayDifference = abs(now.getDayOfYear - date.getDayOfYear)

      if (date.getDayOfYear == now.getDayOfYear) {
        i18n("facto.today")
      } else if (date.getDayOfYear == now.getDayOfYear - 1) {
        i18n("facto.yesterday")
      } else if (date.getDayOfYear == now.getDayOfYear + 1) {
        i18n("facto.tomorrow")
      } else if (dayDifference < 7) {
        s"$dayOfWeek, $dayMonthString"
      } else {
        dayMonthString
      }
    } else {
      s"$dayMonthString '$yearString"
    }
  }

  def formatDateTime(dateTime: LocalDateTime): String = {
    val zone = ZoneId.of("Europe/Paris")
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").format(dateTime.toLocalDate)
  }

  private def extractDayOfWeek(date: LocalDate)
                              (implicit i18n: I18n): String = {
    val dayAbbrevEnglish = DateTimeFormatter.ofPattern("EEE").format(date).toLowerCase
    i18n(s"facto.date.dayofweek.$dayAbbrevEnglish.abbrev")
  }

  private def extractMonth(date: LocalDate)
                          (implicit i18n: I18n): String = {
    val monthAbbrevEnglish = DateTimeFormatter.ofPattern("MMM").format(date).toLowerCase
    i18n(s"facto.date.month.$monthAbbrevEnglish.abbrev")
  }
}
