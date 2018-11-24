package common.time

import java.time.{Duration, Instant}
import java.time.Month._
import common.time.JavaTimeImplicits._
import org.specs2.matcher.MatchResult
import org.specs2.mutable._

class JvmClockTest extends Specification {

  val jvmClock = new JvmClock

  "nowInstant" in {
    assertEqualWithDelta(jvmClock.nowInstant, Instant.now, Duration.ofMillis(10))

    Thread.sleep(1000)

    assertEqualWithDelta(jvmClock.nowInstant, Instant.now, Duration.ofMillis(10))
  }

  def assertEqualWithDelta(a: Instant, b: Instant, delta: Duration): MatchResult[Instant] = {
    a must beGreaterThan(b - delta)
    a must beLessThan(b + delta)
  }
}
