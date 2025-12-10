package hydro.controllers

import com.google.inject.Inject
import hydro.common.ResourceFiles
import hydro.common.time.Clock
import hydro.models.access.EntityAccess
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

final class JavascriptFiles @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: EntityAccess,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
) extends AbstractController(components)
    with I18nSupport {

  private lazy val localDatabaseWebWorkerResultCache: Result = {
    val projectName = "webworker-client"
    val webworkerAssetUrl = firstExistingVersionedUrlPath(s"$projectName-opt.js", s"$projectName-fastopt.js")
    val webworkerDepsAssetUrl =
      firstExistingVersionedUrlPath(s"$projectName-opt-library.js", s"$projectName-fastopt-library.js")

    Ok(s"""
          |importScripts("${webworkerDepsAssetUrl}");
          |var require = ScalaJSBundlerLibrary.require;
          |var exports = {};
          |var window = self;
          |importScripts("${webworkerAssetUrl}");
      """.stripMargin).as("application/javascript")
  }
  def localDatabaseWebWorker = Action(_ => localDatabaseWebWorkerResultCache)

  private def firstExistingVersionedUrlPath(filenames: String*): String = {
    val filename = filenames
      .find(name => ResourceFiles.exists(s"/public/$name"))
      .getOrElse(
        throw new IllegalArgumentException(s"Could not find any of these files: ${filenames.toVector}")
      )
    controllers.routes.Assets.versioned(filename).path()
  }
}
