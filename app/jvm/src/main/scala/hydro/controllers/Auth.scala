package hydro.controllers

import app.models.access.JvmEntityAccess
import app.models.user.Users
import com.google.inject.Inject
import hydro.controllers.Auth.Forms
import hydro.controllers.helpers.AuthenticatedAction
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.libs.mailer._

final class Auth @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    entityAccess: JvmEntityAccess,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
    mailerClient: MailerClient,
) extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def login(returnTo: String) = Action { implicit request =>
    Ok(views.html.login(Forms.loginForm, returnTo))
  }

  def authenticate(returnTo: String) = Action { implicit request =>
    Forms.loginForm.bindFromRequest.fold(
      formWithErrors => {
        maybeSendLoginFailedEmail(formWithErrors("loginName").value, formWithErrors("password").value)
        BadRequest(views.html.login(formWithErrors, returnTo))
      },
      user => Redirect(returnTo).withSession("username" -> user._1),
    )
  }

  def logout = Action { implicit request =>
    Redirect(hydro.controllers.routes.Auth.login("/")).withNewSession.flashing(
      "message" -> Messages("app.you-are-now-logged-out")
    )
  }

  def amILoggedIn = Action { implicit request =>
    Ok(AuthenticatedAction.getAuthenticatedUser(request).isDefined.toString)
  }

  private def maybeSendLoginFailedEmail(loginName: Option[String], password: Option[String]): Unit = {
    val enabled =
      playConfiguration.getOptional[Boolean]("app.mailer.onFailedLoginAttempt.enable") getOrElse false
    if (enabled) {
      val email = Email(
        subject = playConfiguration.get[String]("app.mailer.onFailedLoginAttempt.subject"),
        from = playConfiguration.get[String]("app.mailer.onFailedLoginAttempt.from"),
        to = Seq(playConfiguration.get[String]("app.mailer.onFailedLoginAttempt.to")),
        bodyText = Some(
          "Failed login attempt:\n" +
            s"- login name: '${loginName.getOrElse("")}'\n" +
            s"- password: ${password.getOrElse("").length} characters"
        ),
      )
      mailerClient.send(email)
    }
  }
}

object Auth {
  // ********** forms ********** //
  object Forms {

    def loginForm(implicit entityAccess: JvmEntityAccess) = Form(
      tuple(
        "loginName" -> nonEmptyText,
        "password" -> text,
      ) verifying ("app.error.invalid-username-or-password", result =>
        result match {
          case (loginName, password) => Users.authenticate(loginName, password)
        })
    )
  }

}
