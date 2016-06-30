package controllers.accounting

import play.api.mvc._

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import play.api.i18n.Messages
import models.User
import models.accounting.UpdateLogs
import models.accounting.config.{Config, Template}
import controllers.helpers.AuthenticatedAction

object GeneralActions extends Controller {

  // ********** actions ********** //
  def search(q: String) = AuthenticatedAction { implicit user =>
    implicit request =>
    Ok(q)
  }

  def updateLogsLatest = AuthenticatedAction { implicit user =>
    implicit request =>
      updateLogs(numEntriesToShow = 400)
  }

  def updateLogsAll = AuthenticatedAction { implicit user =>
    implicit request =>
      updateLogs()
  }

  def templateList = AuthenticatedAction { implicit user =>
    implicit request =>
      Ok(views.html.accounting.templatelist(
        templates = Config.templatesToShowFor(Template.Placement.TemplateList, user)))
  }

  // ********** private helper controllers ********** //
  private def updateLogs(numEntriesToShow: Int = 100000)(implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val entries = UpdateLogs.fetchLastNEntries(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.updateLogs(
      entries = entries,
      numEntriesToShow = numEntriesToShow))
  }
}
