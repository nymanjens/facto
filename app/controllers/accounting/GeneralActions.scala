package controllers.accounting

import play.api.mvc._

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import models.User
import models.accounting.UpdateLogs
import controllers.Secured

object GeneralActions extends Controller with Secured {

  // ********** actions ********** //
  def updateLogsLatest = ActionWithUser { implicit user =>
    implicit request =>
      updateLogs(numEntriesToShow = 400)
  }

  def updateLogsAll = ActionWithUser { implicit user =>
    implicit request =>
      updateLogs()
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
