package hydro.common

import java.time.Instant

import app.api.ScalaJsApi.UpdateToken
import hydro.common.GuavaReplacement.Splitter

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
