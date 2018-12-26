package controllers

import com.google.inject.Inject
import controllers.Auth.Forms
import app.models.access.JvmEntityAccess
import app.models.user.Users
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc._

final class Auth @Inject()(implicit override val messagesApi: MessagesApi,
                           components: ControllerComponents,
                           entityAccess: JvmEntityAccess,
                           playConfiguration: play.api.Configuration,
                           env: play.api.Environment)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def login(returnTo: String) = Action { implicit request =>
    Ok(views.html.login(Forms.loginForm, returnTo))
  }

  def authenticate(returnTo: String) = Action { implicit request =>
    Forms.loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, returnTo)),
      user => Redirect(returnTo).withSession("username" -> user._1)
    )
  }

  def logout = Action { implicit request =>
    Redirect(routes.Auth.login("/")).withNewSession.flashing(
      "message" -> Messages("app.you-are-now-logged-out")
    )
  }
}

object Auth {
  // ********** forms ********** //
  object Forms {

    def loginForm(implicit entityAccess: JvmEntityAccess) = Form(
      tuple(
        "loginName" -> nonEmptyText,
        "password" -> text
      ) verifying ("app.error.invalid-username-or-password", result =>
        result match {
          case (loginName, password) => Users.authenticate(loginName, password)
      })
    )
  }

}
