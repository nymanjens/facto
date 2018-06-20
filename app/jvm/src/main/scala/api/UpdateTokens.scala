package api

import java.time.{LocalDate, LocalTime}

import api.Picklers._
import api.ScalaJsApi.UpdateToken
import common.GuavaReplacement.Splitter
import common.time.LocalDateTime

import scala.collection.immutable.Seq

object UpdateTokens {

  def toUpdateToken(dateTime: LocalDateTime): UpdateToken = {
    val epochDay = dateTime.toLocalDate.toEpochDay
    val secondOfDay = dateTime.toLocalTime.toSecondOfDay
    s"$epochDay:$secondOfDay"
  }

  def toLocalDateTime(updateToken: UpdateToken): LocalDateTime = {
    val Seq(epochDay, secondOfDay) = Splitter.on(':').split(updateToken)
    LocalDateTime.of(LocalDate.ofEpochDay(epochDay.toLong), LocalTime.ofSecondOfDay(secondOfDay.toLong))
  }
}
