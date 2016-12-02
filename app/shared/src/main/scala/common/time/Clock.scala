package common.time

import java.time.LocalDateTime

trait Clock {

  def now: LocalDateTime
}
