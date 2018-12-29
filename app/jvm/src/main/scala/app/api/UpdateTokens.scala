package app.api

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

import app.api.Picklers._
import app.api.ScalaJsApi.UpdateToken
import app.common.GuavaReplacement.Splitter
import hydro.common.time.LocalDateTime

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
