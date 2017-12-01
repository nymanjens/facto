package models.accounting.config

import common.Require.requireNonNull
import models._
import models.accounting.config.MoneyReservoir.NullMoneyReservoir
import models.user.User

import scala.collection.immutable.{ListMap, Seq}

/**
  * Contains the accountin configuration of this application. This is assumed to remain constant.
  *
  * @param accounts   Maps code to account
  * @param categories Maps code to category
  */
case class Config(accounts: ListMap[String, Account],
                  categories: ListMap[String, Category],
                  moneyReservoirsMap: ListMap[String, MoneyReservoir],
                  templates: Seq[Template],
                  constants: Constants) {
  requireNonNull(accounts, categories, moneyReservoirsMap, templates, constants)

  // Not exposing moneyReservoirs because it's too easy to accidentally show hidden reservoirs
  def moneyReservoir(code: String): MoneyReservoir = moneyReservoirOption(code).get
  def moneyReservoirOption(code: String): Option[MoneyReservoir] = code match {
    case "" => Some(NullMoneyReservoir)
    case _ => moneyReservoirsMap.get(code)
  }

  def moneyReservoirs(includeNullReservoir: Boolean = false,
                      includeHidden: Boolean = false): Seq[MoneyReservoir] = {
    var result = moneyReservoirsMap.values.toVector
    if (!includeHidden) {
      result = result.filter(!_.hidden)
    }
    if (includeNullReservoir) {
      result ++= Seq(NullMoneyReservoir)
    }
    result
  }

  // Shortcuts for moneyReservoirs(), added because visibleReservoirs makes it clearer that only visible reservoirs
  // are returned than the equivalent moneyReservoirs()
  val visibleReservoirs: Seq[MoneyReservoir] = moneyReservoirs()
  def visibleReservoirs(includeNullReservoir: Boolean = false): Seq[MoneyReservoir] =
    moneyReservoirs(includeNullReservoir = includeNullReservoir)

  def templatesToShowFor(location: Template.Placement, user: User)(
      implicit entityAccess: EntityAccess): Seq[Template] = {
    implicit val accountingConfig = this
    templates filter (_.showFor(location, user))
  }

  def templateWithCode(code: String): Template = {
    val codeToTemplate = {
      for (tpl <- templates) yield tpl.code -> tpl
    }.toMap
    codeToTemplate(code)
  }

  def accountOf(user: User)(implicit entityAccess: EntityAccess): Option[Account] =
    accounts.values.filter(_.userLoginName == Some(user.loginName)).headOption

  def personallySortedAccounts(implicit user: User, entityAccess: EntityAccess): Seq[Account] = {
    val myAccount = accountOf(user)
    val otherAccounts = for {
      acc <- accounts.values
      if !Set(Some(constants.commonAccount), myAccount).flatten.contains(acc)
    } yield acc
    Seq(List(constants.commonAccount), myAccount.toList, otherAccounts).flatten
  }

  def accountsSeq: Seq[Account] = accounts.values.toVector
  def categoriesSeq: Seq[Category] = categories.values.toVector
}
