package controllers

import models._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

object Auth extends Controller {

  // ********** actions ********** //
  def login = Action { implicit request =>
    Ok(views.html.login(Forms.loginForm))
  }

  def authenticate = Action { implicit request =>
    Forms.loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "message" -> "You are now logged out."
    )
  }

  // ********** forms ********** //
  object Forms {

    val loginForm = Form(
      tuple(
        "loginName" -> nonEmptyText,
        "password" -> text
      ) verifying("Invalid email or password", result => result match {
        case (loginName, password) => Users.authenticate(loginName, password)
      })
    )
  }

}

trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  def ActionWithAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def ActionWithUser(f: User => Request[AnyContent] => Result) = ActionWithAuth { username =>
    implicit request =>
      Users.findByLoginName(username).map { user =>
        f(user)(request)
      }.getOrElse(onUnauthorized(request))
  }

  def ActionWithAdminUser(f: User => Request[AnyContent] => Result) = ActionWithUser { implicit user =>
    implicit request =>
      require(user.loginName == "admin")
      f(user)(request)
  }
}
