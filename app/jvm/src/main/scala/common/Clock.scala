package common

import java.time.Instant

object Clock {

  @volatile private var timeOverride: Option[Instant] = None

  def now: Instant = timeOverride match {
    case Some(date) => date
    case None => Instant.now()
  }

  def setTimeForTest(date: Instant): Unit = {
    timeOverride = Some(date)
  }

  def cleanupAfterTest(): Unit = {
    timeOverride = None
  }
}
