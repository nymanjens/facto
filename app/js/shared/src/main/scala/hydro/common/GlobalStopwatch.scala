package hydro.common

import java.time.Instant

import hydro.common.time.JavaTimeImplicits._
import org.scalajs.dom.console

object GlobalStopwatch {

  private var startTime: Instant = Instant.now()
  private var lastLogTime: Instant = Instant.now()

  def startAndLog(stepName: => String): Unit = {
    console.log(s"  {GlobalStopwatch} Starting timer ($stepName)")
    startTime = Instant.now()
    lastLogTime = Instant.now()
  }

  def logTimeSinceStart(stepName: => String): Unit = {
    val now = Instant.now()
    val lastDiff = lastLogTime - now
    val startDiff = startTime - now
    console.log(
      s"  {GlobalStopwatch} Elapsed: Since last time: ${lastDiff.toMillis}ms, Since start: ${startDiff.toMillis}ms ($stepName) ")

    lastLogTime = Instant.now()
  }
}
