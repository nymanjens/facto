package controllers

import com.google.inject.Inject

import scala.collection.immutable.Seq
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import play.Play.application
import play.api.i18n.{MessagesApi, Messages, I18nSupport}

import common.cache.CacheRegistry
import models.{UserManager, User, EntityAccess}
import controllers.accounting.Views
import controllers.helpers.{ControllerHelperCache, AuthenticatedAction}
import controllers.Application.Forms
import controllers.Application.Forms.{AddUserData, ChangePasswordData}

final class Application @Inject()(implicit val messagesApi: MessagesApi,
                            userManager: UserManager,
                            entityAccess: EntityAccess) extends Controller with I18nSupport {

  // ********** actions ********** //
  def index() = AuthenticatedAction { implicit user =>
    implicit request =>
      Redirect(controllers.accounting.routes.Views.cashFlowOfAll)
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
            userManager.update(user.withPasswordHashFromUnhashed(password))
            val message = Messages("facto.successfully-updated-password")
            Redirect(routes.Application.profile).flashing("message" -> message)
        }
      )
  }

  def administration() = AuthenticatedAction.requireAdminUser { implicit user =>
    implicit request =>
      Ok(views.html.administration(users = userManager.fetchAll(), Forms.addUserForm))
  }

  def addUser() = AuthenticatedAction.requireAdminUser { implicit user =>
    implicit request =>
      Forms.addUserForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.administration(users = userManager.fetchAll(), formWithErrors)),
        formData => formData match {
          case AddUserData(loginName, name, password, _) =>
            userManager.add(userManager.newWithUnhashedPw(loginName, password, name))
            val message = Messages("facto.successfully-added-user", name)
            Redirect(routes.Application.administration).flashing("message" -> message)
        }
      )
  }
}

object Application {
  // ********** forms ********** //
  object Forms {

    case class ChangePasswordData(loginName: String,
                                  oldPassword: String = "",
                                  password: String = "",
                                  passwordVerification: String = "")

    def changePasswordForm(implicit entityAccess: EntityAccess) = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "oldPassword" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(ChangePasswordData.apply)(ChangePasswordData.unapply) verifying("facto.error.old-password-is-incorrect", result => result match {
        case ChangePasswordData(loginName, oldPassword, _, _) => entityAccess.userManager.authenticate(loginName, oldPassword)
      }) verifying("facto.error.passwords-should-match", result => result match {
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
      )(AddUserData.apply)(AddUserData.unapply) verifying("facto.error.passwords-should-match", result => result match {
        case AddUserData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )
  }
}
