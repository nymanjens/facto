package controllers

import scala.collection.immutable.Seq
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import play.Play.application
import play.api.i18n.Messages

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import common.cache.CacheRegistry
import models.{Tables, Users, User}
import controllers.accounting.Views
import controllers.helpers.{ControllerHelperCache, AuthenticatedAction}
import controllers.Application.Forms.{AddUserData, ChangePasswordData}

object ExternalApi extends Controller {

  // ********** actions ********** //
  def doCacheManagement(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)
    CacheRegistry.doMaintenanceAndVerifyConsistency()
    Ok("OK")
  }

  /** Warms up caches by rendering the most used views. */
  def warmUpCaches(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val admin: User = Users.findByLoginName("admin").get
    val actions: Seq[AuthenticatedAction] = Seq(
      Views.everythingLatest,
      Views.cashFlowOfAll,
      Views.liquidationOfAll,
      Views.endowmentsOfAll,
      Views.summaryForCurrentYear()
    )
    for (action <- actions) {
      action.calculateResult(admin, request)
    }

    Ok("OK")
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String) = {
    val realApplicationSecret = application.configuration.getString("play.crypto.secret")
    require(applicationSecret == realApplicationSecret, "Invalid application secret")
  }
}
