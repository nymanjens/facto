package common

import org.joda.time.DateTime

object Clock {

  @volatile private var timeOverride: Option[DateTime] = None

  def now: DateTime = timeOverride match {
    case Some(date) => date
    case None => new DateTime()
  }

  def setTimeForTest(date: DateTime): Unit = {
    timeOverride = Some(date)
  }

  def cleanupAfterTest: Unit = {
    timeOverride = None
  }
}
