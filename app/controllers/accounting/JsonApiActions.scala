package controllers.accounting

import common.Clock
import controllers.helpers.accounting.GeneralEntry
import models.accounting.Transactions
import models.accounting.money.{Currency, DatedMoney}
import org.joda.time.DateTime
import play.api.data.{FormError, Forms}
import play.api.mvc._

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import play.api.i18n.Messages
import models.User
import play.api.libs.json.Json
import models.accounting.TagEntities
import models.accounting.config.{Config, Template}
import controllers.helpers.AuthenticatedAction
import models.SlickUtils.dbApi._
import models.SlickUtils.{JodaToSqlDateMapper, dbRun}

object JsonApiActions extends Controller {

  // ********** actions ********** //
  def filterDescriptions(beneficiaryCode: String, reservoirCode: String, categoryCode: String, query: String) =
    AuthenticatedAction { implicit user =>
      implicit request =>
        val descriptions = dbRun(
          Transactions.newQuery
            .filter(_.beneficiaryAccountCode === beneficiaryCode)
            .filter(_.moneyReservoirCode === reservoirCode)
            .filter(_.categoryCode === categoryCode)
            .filter(_.description.toLowerCase startsWith query.toLowerCase)
            .sortBy(r => (r.createdDate.desc))
            .map(_.description)
            .take(50))
          .distinct
          .take(10)
        Ok(Json.toJson(descriptions))
    }

  def getAllTags = AuthenticatedAction { implicit user =>
    implicit request =>
      val tagNames = TagEntities.fetchAll().map(_.tag.name).toSet
      Ok(Json.toJson(tagNames))
  }

  def addBalanceCheck(moneyReservoirCode: String, balanceInCents: Long, mostRecentTransactionId: Long) =
    AuthenticatedAction { implicit user =>
      implicit request =>
        BalanceCheckOperations.doAddConfirmation(moneyReservoirCode, balanceInCents, mostRecentTransactionId)
        Ok(Json.toJson("OK"))
    }

  def exchangeMoney(fromCents: Long, fromCurrencySymbol: String, dateString: String, toCurrencySymbol: String) =
    AuthenticatedAction { implicit user =>
      implicit request =>
        val date: DateTime = {
          val parsedDate: Either[Seq[FormError], DateTime] = Forms.jodaDate("yyyy-MM-dd").bind(Map("" -> dateString))
          parsedDate match {
            case Left(error) => throw new IllegalArgumentException(error.toString)
            case Right(date) => date
          }
        }

        val fromMoney = DatedMoney(fromCents, Currency.of(fromCurrencySymbol), date)
        val toMoney = fromMoney.exchangedForCurrency(Currency.of(toCurrencySymbol))
        Ok(Json.toJson(toMoney.cents))
    }
}
