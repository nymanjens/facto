package hydro.controllers

import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.controllers.helpers.AuthenticatedAction
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

final class StandardActions @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    entityAccess: JvmEntityAccess,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
) extends AbstractController(components)
    with I18nSupport {

  def index() = AuthenticatedAction { implicit user => implicit request =>
    Redirect(hydro.controllers.routes.StandardActions.reactAppRoot())
  }

  def reactAppRoot = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.reactApp())
  }

  def reactApp(anyString: String) = reactAppRoot

  def reactAppWithoutCredentials = Action { implicit request =>
    Ok(views.html.reactApp())
  }

  def healthCheck = Action { implicit request =>
    entityAccess.checkConsistentCaches()
    Ok("OK")
  }

  def databaseSchema(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val schema = entityAccess.getDatabaseSchemaAsStrings().mkString("\n\n")

    Ok(s"OK\n\n$schema")
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'",
    )
  }
}
