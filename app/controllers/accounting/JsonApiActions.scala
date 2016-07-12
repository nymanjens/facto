package controllers.accounting

import controllers.helpers.accounting.GeneralEntry
import models.accounting.Transactions
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
import models.SlickUtils.dbRun

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
            .take(5))
          .map(_.description)
          .toSet
        Ok(Json.toJson(descriptions))
    }

  def getAllTags = AuthenticatedAction { implicit user =>
    implicit request =>
      val tagNames = TagEntities.fetchAll().map(_.tag.name).toSet
      Ok(Json.toJson(tagNames))
  }
}
