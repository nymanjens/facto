package models.accounting.config

import collection.immutable.Seq

import com.google.common.base.Preconditions._
import play.twirl.api.Html

import common.Require.requireNonNullFields
import models.{User, Users}
import Account.SummaryTotalRowDef

case class Account(code: String,
                   longName: String,
                   shorterName: String,
                   veryShortName: String,
                   private val userLoginName: Option[String] = None,
                   private val defaultCashReservoirCode: Option[String] = None,
                   private val defaultElectronicReservoirCode: String,
                   categories: Seq[Category] = Nil,
                   summaryTotalRows: Seq[SummaryTotalRowDef] = Nil) {
  requireNonNullFields(this)

  private[config] def validateCodes(moneyReservoirs: Iterable[MoneyReservoir]): Unit = {
    def requireValidCode(code: String): Unit = {
      val moneyReservoirCodes = moneyReservoirs.map(_.code).toSet
      require(moneyReservoirCodes contains code, s"Unknown code '$code', valid codes = $moneyReservoirCodes")
    }
    defaultCashReservoirCode foreach requireValidCode
    requireValidCode(defaultElectronicReservoirCode)
  }

  override def toString = s"Account($code)"

  def user: Option[User] = {
    userLoginName.map {
      loginName =>
        val user = Users.findByLoginName(loginName)
        checkState(user.isDefined, "No user exists with loginName '%s'", loginName)
        user.get
    }
  }
  def defaultCashReservoir: Option[MoneyReservoir] = defaultCashReservoirCode map Config.moneyReservoir
  def defaultElectronicReservoir: MoneyReservoir = Config.moneyReservoir(defaultElectronicReservoirCode)
  def visibleReservoirs: Seq[MoneyReservoir] = Config.visibleReservoirs.filter(_.owner == this).toList
  def isMineOrCommon(implicit user: User): Boolean = Set(Config.accountOf(user), Some(Config.constants.commonAccount)).flatten.contains(this)
}

object Account {
  case class SummaryTotalRowDef(rowTitleHtml: Html, categoriesToIgnore: Set[Category]) {
    requireNonNullFields(this)
  }
}
