package hydro.common

import java.time.Instant

import hydro.common.time.JavaTimeImplicits._

object GlobalStopwatch {

  private var startTime: Instant = Instant.now()
  private var lastLogTime: Instant = Instant.now()

  def startAndLog(stepName: => String): Unit = {
    println(s"  {GlobalStopwatch} Starting timer ($stepName)")
    startTime = Instant.now()
    lastLogTime = Instant.now()
  }

  def logTimeSinceStart(stepName: => String): Unit = {
    val now = Instant.now()
    val lastDiff = now - lastLogTime
    val startDiff = now - startTime
    println(
      s"  {GlobalStopwatch} Elapsed: Since last time: ${lastDiff.toMillis}ms, Since start: ${startDiff.toMillis}ms ($stepName) ")

    lastLogTime = Instant.now()
  }
}
