package app.common.accounting

import app.common.money.MoneyWithGeneralCurrency
import app.common.time.DatedMonth
import hydro.common.time.LocalDateTime
import hydro.common.time.JavaTimeImplicits._

import java.time.Duration
import java.time.LocalTime
import scala.collection.immutable.SortedMap
import scala.collection.mutable

final class DateToBalanceFunction(
    dateToBalanceUpdates: SortedMap[LocalDateTime, DateToBalanceFunction.Update]
) {
  def apply(date: LocalDateTime): MoneyWithGeneralCurrency = {
    dateToBalanceUpdates.to(date).values.last.balance
  }

  def updatesInRange(month: DatedMonth): SortedMap[LocalDateTime, DateToBalanceFunction.Update] = {
    dateToBalanceUpdates.range(month.startTime, month.startTimeOfNextMonth)
  }

  def updatesInRange(
      start: LocalDateTime,
      end: LocalDateTime,
  ): SortedMap[LocalDateTime, DateToBalanceFunction.Update] = {
    dateToBalanceUpdates.range(start, end)
  }
}

object DateToBalanceFunction {
  case class Update(balance: MoneyWithGeneralCurrency, changeComparedToLast: MoneyWithGeneralCurrency)

  final class Builder(initialDate: LocalDateTime, initialBalance: MoneyWithGeneralCurrency) {
    private val dateToBalanceUpdates: mutable.SortedMap[LocalDateTime, Update] =
      mutable.SortedMap(
        initialDate -> Update(balance = initialBalance, changeComparedToLast = initialBalance)
      )

    def incrementLatestBalance(date: LocalDateTime, addition: MoneyWithGeneralCurrency): Unit = {
      val (lastDate, lastBalance) = dateToBalanceUpdates.last
      dateToBalanceUpdates.put(
        maybeAddSomeSecondsToAvoidEdgeCases(date),
        Update(balance = lastBalance.balance + addition, changeComparedToLast = addition),
      )
    }

    def addBalanceUpdate(date: LocalDateTime, balance: MoneyWithGeneralCurrency): Unit = {
      val (lastDate, lastBalance) = dateToBalanceUpdates.last
      dateToBalanceUpdates.put(
        maybeAddSomeSecondsToAvoidEdgeCases(date),
        Update(balance = balance, changeComparedToLast = balance - lastBalance.balance),
      )
    }

    def result: DateToBalanceFunction = {
      new DateToBalanceFunction(SortedMap.apply(dateToBalanceUpdates.toSeq: _*))
    }

    /** Avoids duplicates dates and 00:00. Assumes that dates are provided to this builder in chronological order. */
    private def maybeAddSomeSecondsToAvoidEdgeCases(date: LocalDateTime): LocalDateTime = {
      var candidate = date

      // Avoid LocalDateTimes that start at '00:00', which on the first day of the month interferes
      // with DatedMonth.startOfNextMonth.
      candidate = if (candidate.toLocalTime == LocalTime.MIN) {
        candidate.plus(Duration.ofSeconds(1))
      } else {
        candidate
      }

      // Avoid dates that are already in the dateToBalanceUpdates map (assuming dates were presented in
      // chronological order)
      val (lastDate, lastBalance) = dateToBalanceUpdates.last
      candidate =
        if (lastDate < candidate) candidate
        else lastDate.plus(Duration.ofSeconds(1))

      candidate
    }
  }
}
