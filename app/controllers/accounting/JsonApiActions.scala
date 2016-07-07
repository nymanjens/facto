package controllers.accounting

import controllers.helpers.accounting.GeneralEntry
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

object JsonApiActions extends Controller {

  // ********** actions ********** //
  def getAllTags = AuthenticatedAction { implicit user =>
    implicit request =>
      val tagNames = TagEntities.fetchAll().map(_.tag.name)
      Ok(Json.toJson(tagNames))
  }
}
