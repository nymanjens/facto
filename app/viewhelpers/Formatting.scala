package viewhelpers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Formatting {

  def formatDate(date: DateTime) = {
    date.toString(DateTimeFormat.forPattern("dd MMM"))
  }

  def formatDateTime(date: DateTime) = {
    date.toString(DateTimeFormat.forPattern("d MMM yyyy, HH:mm"))
  }
}
