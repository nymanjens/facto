package controllers.helpers

import models._
import play.api.mvc.{RequestHeader, Action, Result, AnyContent, Request, EssentialAction, Security, Results}
import play.api.libs.iteratee.Iteratee

import controllers.helpers.AuthenticatedAction.UserAndRequestToResult

abstract class AuthenticatedAction(implicit entityAccess: EntityAccess) extends EssentialAction {

  private val delegate: EssentialAction = {
    Security.Authenticated(username, onUnauthorized) { username =>
      Action { request =>
        entityAccess.userManager.findByLoginName(username) match {
          case Some(user) => calculateResult(user, request)
          case None => onUnauthorized(request)
        }
      }
    }
  }

  override def apply(requestHeader: RequestHeader) = delegate.apply(requestHeader)

  def calculateResult(implicit user: User, request: Request[AnyContent]): Result

  private def username(request: RequestHeader): Option[String] = request.session.get(Security.username)

  private def onUnauthorized(request: RequestHeader): Result = Results.Redirect(controllers.routes.Auth.login)
}

object AuthenticatedAction {

  type UserAndRequestToResult = User => Request[AnyContent] => Result

  def apply(userAndRequestToResult: UserAndRequestToResult)(implicit entityAccess: EntityAccess): AuthenticatedAction = {
    new AuthenticatedAction {
      override def calculateResult(implicit user: User, request: Request[AnyContent]): Result = {
        userAndRequestToResult(user)(request)
      }
    }
  }

  def requireAdminUser(userAndRequestToResult: UserAndRequestToResult)(implicit entityAccess: EntityAccess): AuthenticatedAction =
    apply { user =>
      request =>
        require(user.loginName == "admin")
        userAndRequestToResult(user)(request)
    }
}
