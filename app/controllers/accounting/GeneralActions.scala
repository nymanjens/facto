package controllers.accounting

import controllers.helpers.accounting.GeneralEntry
import play.api.mvc._

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import play.api.i18n.Messages
import models.User
import models.accounting.UpdateLogs
import models.accounting.config.{Config, Template}
import controllers.helpers.AuthenticatedAction

class GeneralActions extends Controller {

  // ********** actions ********** //
  def searchMostRelevant(q: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      search(q, numEntriesToShow = 200)
  }

  def searchAll(q: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      search(q)
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
  private def search(query: String, numEntriesToShow: Int = 100000)
                    (implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val allEntries = GeneralEntry.search(query)
    val entries = allEntries.take(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.search(
      query = query,
      totalNumResults = allEntries.size,
      entries = entries,
      numEntriesToShow = numEntriesToShow,
      templatesInNavbar = Config.templatesToShowFor(Template.Placement.SearchView, user)))
  }

  private def updateLogs(numEntriesToShow: Int = 100000)(implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val entries = UpdateLogs.fetchLastNEntries(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.updateLogs(
      entries = entries,
      numEntriesToShow = numEntriesToShow))
  }
}
