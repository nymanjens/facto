package controllers

import scala.collection.immutable.Seq

import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import play.Play.application

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import common.cache.CacheMaintenanceManager
import models.{Tables, Users, User}
import controllers.accounting.Views
import controllers.helpers.{HelperCache, AuthenticatedAction}
import controllers.Application.Forms.{AddUserData, ChangePasswordData}

object Application extends Controller {

  // ********** actions ********** //
  def index() = AuthenticatedAction { implicit user =>
    implicit request =>
      Redirect(controllers.accounting.routes.Views.cashFlowOfAll)
  }

  def doCacheManagement(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)
    CacheMaintenanceManager.doMaintenanceAndVerifyConsistency()
    Ok("OK")
  }

  /** Warms up caches by rendering the most used views. */
  def warmUpCaches(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val admin: User = Users.findByLoginName("admin").get
    val actions: Seq[AuthenticatedAction] = Seq(
      Views.generalLatest,
      Views.cashFlowOfAll,
      Views.liquidationOfAll,
      Views.endowmentsOfAll,
      Views.summaryForCurrentYear
    )
    for (action <- actions) {
      action.calculateResult(admin, request)
    }

    Ok("OK")
  }

  def profile() = AuthenticatedAction { implicit user =>
    implicit request =>
      val initialData = ChangePasswordData(user.loginName)
      Ok(views.html.profile(Forms.changePasswordForm.fill(initialData)))
  }

  def changePassword = AuthenticatedAction { implicit user =>
    implicit request =>
      Forms.changePasswordForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.profile(formWithErrors)),
        formData => formData match {
          case ChangePasswordData(loginName, _, password, _) =>
            require(loginName == user.loginName)
            Users.add(user.withPasswordHashFromUnhashed(password))
            val message = "Successfully updated password"
            Redirect(routes.Application.profile).flashing("message" -> message)
        }
      )
  }

  def administration() = AuthenticatedAction.requireAdminUser { implicit user =>
    implicit request =>
      Ok(views.html.administration(users = Users.fetchAll(), Forms.addUserForm))
  }

  def addUser() = AuthenticatedAction.requireAdminUser { implicit user =>
    implicit request =>
      Forms.addUserForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.administration(users = Users.fetchAll(), formWithErrors)),
        formData => formData match {
          case AddUserData(loginName, name, password, _) =>
            Users.add(Users.newWithUnhashedPw(loginName, password, name))
            val message = s"Successfully added user $name"
            Redirect(routes.Application.administration).flashing("message" -> message)
        }
      )
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String) = {
    val realApplicationSecret = application.configuration.getString("play.crypto.secret")
    require(applicationSecret == realApplicationSecret, "Invalid application secret")
  }

  // ********** forms ********** //
  object Forms {

    case class ChangePasswordData(loginName: String,
                                  oldPassword: String = "",
                                  password: String = "",
                                  passwordVerification: String = "")

    val changePasswordForm = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "oldPassword" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(ChangePasswordData.apply)(ChangePasswordData.unapply) verifying("Old password is incorrect", result => result match {
        case ChangePasswordData(loginName, oldPassword, _, _) => Users.authenticate(loginName, oldPassword)
      }) verifying("Passwords should match", result => result match {
        case ChangePasswordData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )

    case class AddUserData(loginName: String,
                           name: String = "",
                           password: String = "",
                           passwordVerification: String = "")

    val addUserForm = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "name" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(AddUserData.apply)(AddUserData.unapply) verifying("Passwords should match", result => result match {
        case AddUserData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )
  }
}
