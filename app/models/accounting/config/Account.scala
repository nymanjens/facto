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
                   categories: Seq[Category] = Nil,
                   summaryTotalRows: Seq[SummaryTotalRowDef] = Nil) {
  requireNonNullFields(this)

  override def toString = s"Account($code)"

  def user: Option[User] = {
    userLoginName.map { loginName =>
      val user = Users.findByLoginName(loginName)
      checkState(user.isDefined, "No user exists with loginName '%s'", loginName)
      user.get
    }
  }

  def visibleReservoirs: Seq[MoneyReservoir] = Config.visibleReservoirs.filter(_.owner == this).toList

  def isMineOrCommon(implicit user: User): Boolean = Set(Config.accountOf(user), Some(Config.constants.commonAccount)).flatten.contains(this)
}

object Account {
  case class SummaryTotalRowDef(rowTitleHtml: Html, categoriesToIgnore: Set[Category]) {
    requireNonNullFields(this)
  }
}
