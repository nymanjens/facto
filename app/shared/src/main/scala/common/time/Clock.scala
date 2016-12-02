package common.time

import common.time.LocalDateTime

trait Clock {

  def now: LocalDateTime
}
