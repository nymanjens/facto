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

case class AccountingYear(startYear: Int, startMonth: Month) extends Ordered[AccountingYear] {
  override def compare(that: AccountingYear): Int = this.startYear compareTo that.startYear
  override def toString = {
    if (startMonth == Month.JANUARY) {
      startYear.toString
    } else {
      s"$startYear - ${startYear + 1}"
    }
  }
}

object AccountingYear {
  def ofStartYear(year: Int)(implicit accountingConfig: Config): AccountingYear = {
    AccountingYear(startYear = year, startMonth = accountingConfig.constants.firstMonthOfYear)
  }
}
