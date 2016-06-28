package controllers

import models._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages

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

  def logout = Action { implicit request =>
    Redirect(routes.Auth.login).withNewSession.flashing(
      "message" -> Messages("facto.you-are-now-logged-out")
    )
  }

  // ********** forms ********** //
  object Forms {

    val loginForm = Form(
      tuple(
        "loginName" -> nonEmptyText,
        "password" -> text
      ) verifying("facto.error.invalid-username-or-password", result => result match {
        case (loginName, password) => Users.authenticate(loginName, password)
      })
    )
  }

}
