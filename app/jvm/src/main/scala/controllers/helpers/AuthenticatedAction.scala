package controllers.helpers

import models._
import play.api.mvc._
import controllers.helpers.AuthenticatedAction.UserAndRequestToResult

abstract class AuthenticatedAction[A](bodyParser: BodyParser[A])(implicit entityAccess: EntityAccess,
                                                                 controllerComponents: ControllerComponents,
                                                                 playConfiguration: play.api.Configuration)
    extends EssentialAction {

  private val delegate: EssentialAction = {
    Security.Authenticated(username, onUnauthorized) { username =>
      controllerComponents.actionBuilder(bodyParser) { request =>
        entityAccess.userManager.findByLoginName(username) match {
          case Some(user) => calculateResult(user, request)
          case None => onUnauthorized(request)
        }
      }
    }
  }

  override def apply(requestHeader: RequestHeader) = delegate.apply(requestHeader)

  def calculateResult(implicit user: User, request: Request[A]): Result

  // **************** private helper methods **************** //
  private def username(request: RequestHeader): Option[String] = request.session.get("username")

  private def onUnauthorized(request: RequestHeader): Result =
    Results.Redirect(controllers.routes.Auth.login)
}

object AuthenticatedAction {

  type UserAndRequestToResult[A] = User => Request[A] => Result

  def apply[A](bodyParser: BodyParser[A])(userAndRequestToResult: UserAndRequestToResult[A])(
      implicit entityAccess: EntityAccess,
      controllerComponents: ControllerComponents,
      playConfiguration: play.api.Configuration): AuthenticatedAction[A] = {
    new AuthenticatedAction[A](bodyParser) {
      override def calculateResult(implicit user: User, request: Request[A]): Result = {
        userAndRequestToResult(user)(request)
      }
    }
  }

  def apply(userAndRequestToResult: UserAndRequestToResult[AnyContent])(
      implicit entityAccess: EntityAccess,
      controllerComponents: ControllerComponents,
      playConfiguration: play.api.Configuration): AuthenticatedAction[AnyContent] = {
    apply(controllerComponents.parsers.defaultBodyParser)(userAndRequestToResult)
  }

  def requireAdminUser(userAndRequestToResult: UserAndRequestToResult[AnyContent])(
      implicit entityAccess: EntityAccess,
      controllerComponents: ControllerComponents,
      playConfiguration: play.api.Configuration): AuthenticatedAction[AnyContent] =
    AuthenticatedAction { user => request =>
      require(user.loginName == "admin")
      userAndRequestToResult(user)(request)
    }
}
