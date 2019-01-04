package app.controllers

import app.api.ScalaJsApiServerFactory
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.controllers.helpers.AuthenticatedAction
import hydro.common.time.Clock
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

final class Application @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  entityAccess: JvmEntityAccess,
                                  scalaJsApiServerFactory: ScalaJsApiServerFactory,
                                  playConfiguration: play.api.Configuration,
                                  env: play.api.Environment)
    extends AbstractController(components)
    with I18nSupport {

  def manualTests() = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.manualTests())
  }
}
