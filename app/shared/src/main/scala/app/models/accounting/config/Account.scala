package app.models.accounting.config

import app.common.Require.requireNonNull
import app.models.access.AppEntityAccess
import app.models.access.ModelFields
import app.models.accounting.config.Account.SummaryTotalRowDef
import app.models.user.User
import hydro.models.access.DbQueryImplicits._

import scala.collection.immutable.Seq

case class Account(code: String,
                   longName: String,
                   shorterName: String,
                   veryShortName: String,
                   userLoginName: Option[String] = None,
                   defaultCashReservoirCode: Option[String] = None,
                   defaultElectronicReservoirCode: String,
                   categories: Seq[Category] = Nil,
                   summaryTotalRows: Seq[SummaryTotalRowDef] = Nil) {
  requireNonNull(
    code,
    longName,
    shorterName,
    veryShortName,
    userLoginName,
    defaultCashReservoirCode,
    defaultCashReservoirCode,
    categories,
    summaryTotalRows)

  private[config] def validateCodes(moneyReservoirs: Iterable[MoneyReservoir]): Unit = {
    def requireValidCode(code: String): Unit = {
      val moneyReservoirCodes = moneyReservoirs.map(_.code).toSet
      require(moneyReservoirCodes contains code, s"Unknown code '$code', valid codes = $moneyReservoirCodes")
    }
    defaultCashReservoirCode foreach requireValidCode
    requireValidCode(defaultElectronicReservoirCode)
  }

  override def toString = s"Account($code)"

  def user(implicit entityAccess: AppEntityAccess): Option[User] = {
    userLoginName.map { loginName =>
      val user = entityAccess.newQuerySyncForUser().findOne(ModelFields.User.loginName === loginName)
      require(user.isDefined, s"No user exists with loginName '$loginName'")
      user.get
    }
  }

  def defaultCashReservoir(implicit accountingConfig: Config): Option[MoneyReservoir] =
    defaultCashReservoirCode map accountingConfig.moneyReservoir

  def defaultElectronicReservoir(implicit accountingConfig: Config): MoneyReservoir =
    accountingConfig.moneyReservoir(defaultElectronicReservoirCode)

  def visibleReservoirs(implicit accountingConfig: Config): Seq[MoneyReservoir] =
    accountingConfig.visibleReservoirs.filter(_.owner == this).toList

  def isMineOrCommon(implicit user: User, accountingConfig: Config, entityAccess: AppEntityAccess): Boolean =
    Set(accountingConfig.accountOf(user), Some(accountingConfig.constants.commonAccount)).flatten
      .contains(this)
}

object Account {
  val nullInstance = Account(
    code = "NULL_INSTANCE",
    longName = "NULL_INSTANCE",
    shorterName = "NULL_INSTANCE",
    veryShortName = "NULL_INSTANCE",
    defaultElectronicReservoirCode = "")

  case class SummaryTotalRowDef(rowTitleHtml: String, categoriesToIgnore: Set[Category]) {
    requireNonNull(rowTitleHtml, categoriesToIgnore)
  }
}
