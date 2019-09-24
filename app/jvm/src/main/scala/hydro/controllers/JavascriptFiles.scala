package hydro.controllers

import java.net.URL

import hydro.controllers.JavascriptFiles.Asset
import hydro.controllers.JavascriptFiles.appAssets
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.io.Resources
import com.google.inject.Inject
import hydro.common.GuavaReplacement.Splitter
import hydro.common.ResourceFiles
import hydro.common.time.Clock
import hydro.common.Annotations.visibleForTesting
import hydro.models.access.EntityAccess
import play.api.Mode
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.annotation.StaticAnnotation
import scala.collection.immutable.Seq

final class JavascriptFiles @Inject()(
    implicit override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: EntityAccess,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
    @appAssets appAssets: Seq[Asset],
) extends AbstractController(components)
    with I18nSupport {

  private lazy val localDatabaseWebWorkerResultCache: Result =
    Ok(s"""
          |importScripts("${JavascriptFiles.Assets.webworkerDeps.urlPath}");
          |var require = ScalaJSBundlerLibrary.require;
          |var exports = {};
          |var window = self;
          |importScripts("${JavascriptFiles.Assets.webworker.urlPath}");
      """.stripMargin).as("application/javascript")
  def localDatabaseWebWorker = Action(_ => localDatabaseWebWorkerResultCache)

  private def serviceWorkerResultFunc(): Result = {
    val jsFileTemplate = ResourceFiles.read("/serviceWorker.template.js")
    val scriptPathsJs = allAssets.map(asset => s"'${asset.urlPath}'").mkString(", ")
    val cacheNameSuffix = {
      val hasher = Hashing.murmur3_128().newHasher()
      for (asset <- allAssets) {
        hasher.putString(asset.urlPath, Charsets.UTF_8)
        for (resource <- asset.maybeLocalResource) {
          hasher.putBytes(Resources.toByteArray(resource))
        }
      }
      hasher.hash().toString
    }
    val jsFileContent = jsFileTemplate
      .replace("$SCRIPT_PATHS_TO_CACHE$", scriptPathsJs)
      .replace("$CACHE_NAME_SUFFIX$", cacheNameSuffix)
    Ok(jsFileContent).as("application/javascript").withHeaders("Cache-Control" -> "no-cache")
  }
  private lazy val serviceWorkerResultCache: Result = serviceWorkerResultFunc()
  def serviceWorker =
    Action(_ => if (env.mode == Mode.Dev) serviceWorkerResultFunc() else serviceWorkerResultCache)

  private def allAssets: Seq[Asset] = JavascriptFiles.Assets.standardAssets ++ appAssets
}

object JavascriptFiles {

  class appAssets extends StaticAnnotation

  sealed trait Asset {
    def maybeLocalResource: Option[URL]
    def urlPath: String
  }
  abstract class ResourceAsset(relativePath: String) extends Asset {
    // Remove the query parameters to find the local resource path
    private val resourcePath: String = Splitter.on('?').split(s"/public/$relativePath").head
    require(ResourceFiles.exists(resourcePath), s"Could not find asset at $resourcePath")

    override final def maybeLocalResource = Some(getClass.getResource(resourcePath))
  }
  case class VersionedAsset(relativePath: String) extends ResourceAsset(relativePath) {
    override def urlPath = controllers.routes.Assets.versioned(relativePath).path()
  }
  case class UnversionedAsset(relativePath: String) extends ResourceAsset(relativePath) {
    override def urlPath = s"/assets/$relativePath"
  }
  case class DynamicAsset(call: Call) extends Asset {
    override def maybeLocalResource = None
    override def urlPath = call.path()
  }

  private object Assets {
    private val clientAppProjectName: String = "client"
    private val webworkerProjectName: String = "webworker-client"

    val clientApp: Asset =
      firstExistingVersionedAsset(s"$clientAppProjectName-opt.js", s"$clientAppProjectName-fastopt.js")
    val clientAppDeps: Asset =
      firstExistingVersionedAsset(
        s"$clientAppProjectName-opt-library.js",
        s"$clientAppProjectName-fastopt-library.js")
    val webworker: Asset =
      firstExistingVersionedAsset(s"$webworkerProjectName-opt.js", s"$webworkerProjectName-fastopt.js")
    val webworkerDeps: Asset =
      firstExistingVersionedAsset(
        s"$webworkerProjectName-opt-library.js",
        s"$webworkerProjectName-fastopt-library.js")

    val standardAssets: Seq[Asset] = Seq(
      clientApp,
      clientAppDeps,
      webworker,
      webworkerDeps,
      VersionedAsset("images/favicon192x192.png"),
      DynamicAsset(routes.JavascriptFiles.localDatabaseWebWorker)
    )

    private def firstExistingVersionedAsset(filenames: String*): Asset =
      VersionedAsset(
        filenames
          .find(name => ResourceFiles.exists(s"/public/$name"))
          .getOrElse(
            throw new IllegalArgumentException(s"Could not find any of these files: ${filenames.toVector}")))
  }
}
