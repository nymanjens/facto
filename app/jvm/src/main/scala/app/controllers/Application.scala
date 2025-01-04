package app.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream.scaladsl.StreamConverters
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import app.api.ScalaJsApiServerFactory
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.controllers.helpers.AuthenticatedAction
import hydro.common.time.Clock
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import java.nio.file.Paths

final class Application @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    scalaJsApiServerFactory: ScalaJsApiServerFactory,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
) extends AbstractController(components)
    with I18nSupport {

  def manualTests() = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.manualTests())
  }

  def getAttachment(contentHash: String, typeEncoded: String, filename: String) = AuthenticatedAction {
    implicit user => implicit request =>
      val folder = playConfiguration.get[String]("app.accounting.attachmentsFolder")
      val folderPath = Paths.get(folder)

      val assetPath = folderPath.resolve(contentHash)

      if (!Files.exists(assetPath)) {
        NotFound(s"Could not find $assetPath")
      } else if (Files.isDirectory(assetPath)) {
        NotFound(s"Could not find $assetPath")
      } else {
        Ok.sendPath(assetPath, inline = true, fileName = _ => Some(filename))
      }
  }
}
