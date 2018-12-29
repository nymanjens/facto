package hydro.common.time

import java.time.Duration
import java.time.Instant

import com.google.inject._
import app.common.testing.HookedSpecification
import app.common.testing.TestModule
import hydro.common.time.JavaTimeImplicits._
import org.specs2.matcher.MatchResult

class JvmClockTest extends HookedSpecification {

  @Inject private val jvmClock: JvmClock = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

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
