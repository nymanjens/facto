package controllers

import api.Picklers._
import api.ScalaJsApiServerFactory
import com.google.inject.Inject
import common.time.Clock
import controllers.Application.Forms
import controllers.Application.Forms.{AddUserData, ChangePasswordData}
import controllers.helpers.AuthenticatedAction
import models.access.JvmEntityAccess
import models.modification.EntityModification
import models.user.{User, Users}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

final class Application @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  entityAccess: JvmEntityAccess,
                                  scalaJsApiServerFactory: ScalaJsApiServerFactory,
                                  playConfiguration: play.api.Configuration,
                                  env: play.api.Environment,
                                  webJarAssets: controllers.WebJarAssets)
    extends AbstractController(components)
    with I18nSupport {

  def index() = AuthenticatedAction { implicit user => implicit request =>
    Redirect(controllers.routes.Application.reactAppRoot())
  }

  def profile() = AuthenticatedAction { implicit user => implicit request =>
    val initialData = ChangePasswordData(user.loginName)
    Ok(views.html.profile(Forms.changePasswordForm.fill(initialData)))
  }

  def changePassword = AuthenticatedAction { implicit user => implicit request =>
    Forms.changePasswordForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.profile(formWithErrors)), {
        case ChangePasswordData(loginName, _, password, _) =>
          require(loginName == user.loginName)
          entityAccess.persistEntityModifications(
            EntityModification.createUpdate(Users.copyUserWithPassword(user, password)))
          val message = Messages("facto.successfully-updated-password")
          Redirect(routes.Application.profile()).flashing("message" -> message)
      }
    )
  }

  def administration() = AuthenticatedAction.requireAdminUser { implicit user => implicit request =>
    Ok(views.html.administration(users = entityAccess.newQuerySync[User]().data(), Forms.addUserForm))
  }

  def addUser() = AuthenticatedAction.requireAdminUser { implicit user => implicit request =>
    Forms.addUserForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(
          views.html.administration(users = entityAccess.newQuerySync[User]().data(), formWithErrors)), {
        case AddUserData(loginName, name, password, _) =>
          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(Users.createUser(loginName, password, name)))
          val message = Messages("facto.successfully-added-user", name)
          Redirect(routes.Application.administration()).flashing("message" -> message)
      }
    )
  }

  def reactAppRoot = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.reactApp())
  }
  def reactApp(anyString: String) = reactAppRoot

  def reactAppWithoutCredentials = Action { implicit request =>
    Ok(views.html.reactApp())
  }

  def manualTests() = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.manualTests())
  }
}

object Application {

  object Forms {

    case class ChangePasswordData(loginName: String,
                                  oldPassword: String = "",
                                  password: String = "",
                                  passwordVerification: String = "")

    def changePasswordForm(implicit entityAccess: JvmEntityAccess) = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "oldPassword" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(ChangePasswordData.apply)(ChangePasswordData.unapply) verifying ("facto.error.old-password-is-incorrect", result =>
        result match {
          case ChangePasswordData(loginName, oldPassword, _, _) =>
            Users.authenticate(loginName, oldPassword)
      }) verifying ("facto.error.passwords-should-match", result =>
        result match {
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
      )(AddUserData.apply)(AddUserData.unapply) verifying ("facto.error.passwords-should-match", result =>
        result match {
          case AddUserData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )
  }
}
