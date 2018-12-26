package hydro.common.time

import java.time.Instant

trait Clock {

  def now: LocalDateTime
  def nowInstant: Instant
}
