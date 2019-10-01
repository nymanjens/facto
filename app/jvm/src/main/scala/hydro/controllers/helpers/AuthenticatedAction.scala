package hydro.controllers.helpers

import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.user.User
import hydro.models.access.DbQueryImplicits._
import play.api.mvc._

abstract class AuthenticatedAction[A](bodyParser: BodyParser[A])(
    implicit entityAccess: JvmEntityAccess,
    controllerComponents: ControllerComponents,
    playConfiguration: play.api.Configuration,
) extends EssentialAction {

  private val delegate: EssentialAction = {
    Security.Authenticated(AuthenticatedAction.username, AuthenticatedAction.onUnauthorized) { username =>
      controllerComponents.actionBuilder(bodyParser) { request =>
        entityAccess.newQuerySync[User]().findOne(ModelFields.User.loginName === username) match {
          case Some(user) => calculateResult(user, request)
          case None       => AuthenticatedAction.onUnauthorized(request)
        }
      }
    }
  }

  override def apply(requestHeader: RequestHeader) = delegate.apply(requestHeader)

  def calculateResult(implicit user: User, request: Request[A]): Result
}

object AuthenticatedAction {

  type UserAndRequestToResult[A] = User => Request[A] => Result

  def apply[A](bodyParser: BodyParser[A])(userAndRequestToResult: UserAndRequestToResult[A])(
      implicit entityAccess: JvmEntityAccess,
      controllerComponents: ControllerComponents,
      playConfiguration: play.api.Configuration,
  ): AuthenticatedAction[A] = {
    new AuthenticatedAction[A](bodyParser) {
      override def calculateResult(implicit user: User, request: Request[A]): Result = {
        userAndRequestToResult(user)(request)
      }
    }
  }

  def apply(userAndRequestToResult: UserAndRequestToResult[AnyContent])(
      implicit entityAccess: JvmEntityAccess,
      controllerComponents: ControllerComponents,
      playConfiguration: play.api.Configuration,
  ): AuthenticatedAction[AnyContent] = {
    apply(controllerComponents.parsers.defaultBodyParser)(userAndRequestToResult)
  }

  def requireAdminUser(userAndRequestToResult: UserAndRequestToResult[AnyContent])(
      implicit entityAccess: JvmEntityAccess,
      controllerComponents: ControllerComponents,
      playConfiguration: play.api.Configuration,
  ): AuthenticatedAction[AnyContent] =
    AuthenticatedAction { user => request =>
      require(user.isAdmin)
      userAndRequestToResult(user)(request)
    }

  def requireAuthenticatedUser(request: RequestHeader)(implicit entityAccess: JvmEntityAccess): User = {
    val user = getAuthenticatedUser(request)
    require(user.isDefined, s"Request is not authenticated: $request")
    user.get
  }

  def getAuthenticatedUser(request: RequestHeader)(implicit entityAccess: JvmEntityAccess): Option[User] = {
    for {
      username <- AuthenticatedAction.username(request)
      user <- entityAccess.newQuerySync[User]().findOne(ModelFields.User.loginName === username)
    } yield user
  }

  // **************** private helper methods **************** //
  private def username(request: RequestHeader): Option[String] = request.session.get("username")

  private def onUnauthorized(request: RequestHeader): Result =
    Results.Redirect(hydro.controllers.routes.Auth.login(request.uri))
}
