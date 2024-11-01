package app.common.time

import app.models.accounting.config.Config
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.common.time.LocalDateTime
import hydro.common.time.TimeUtils

import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import scala.collection.immutable.Seq
import scala.collection.mutable

case class AccountingYear(startYear: Int) extends Ordered[AccountingYear] {
  override def compare(that: AccountingYear): Int = this.startYear compareTo that.startYear

  def endYear(implicit accountingConfig: Config): Int = {
    if (accountingConfig.constants.firstMonthOfYear == Month.JANUARY) {
      startYear
    } else {
      startYear + 1
    }
  }

  def toHumanReadableString(implicit accountingConfig: Config): String = {
    if (accountingConfig.constants.firstMonthOfYear == Month.JANUARY) {
      startYear.toString
    } else {
      s"$startYear - ${startYear + 1}"
    }
  }

  def minusYears(diff: Int): AccountingYear = {
    AccountingYear(startYear - diff)
  }
}
object AccountingYear {
  def from(date: LocalDateTime)(implicit accountingConfig: Config): AccountingYear = {
    DatedMonth.containing(date).accountingYear
  }

  def current(implicit clock: Clock, accountingConfig: Config): AccountingYear = {
    DatedMonth.current.accountingYear
  }
}
