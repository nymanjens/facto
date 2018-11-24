package api

import java.time.{Instant, LocalDate, LocalTime}

import api.Picklers._
import api.ScalaJsApi.UpdateToken
import common.GuavaReplacement.Splitter
import common.time.LocalDateTime

import scala.collection.immutable.Seq

object UpdateTokens {

  def toUpdateToken(instant: Instant): UpdateToken = {
    s"${instant.getEpochSecond}:${instant.getNano}"
  }

  def toInstant(updateToken: UpdateToken): Instant = {
    val Seq(epochSecond, nano) = Splitter.on(':').split(updateToken)
    Instant.ofEpochSecond(epochSecond.toLong).plusNanos(nano.toLong)
  }
}
