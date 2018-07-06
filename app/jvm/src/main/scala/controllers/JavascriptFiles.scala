package controllers

import java.net.URL

import api.Picklers._
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.io.Resources
import com.google.inject.Inject
import common.GuavaReplacement.Splitter
import common.ResourceFiles
import common.time.Clock
import models.access.JvmEntityAccess
import play.api.Mode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.collection.immutable.Seq

final class JavascriptFiles @Inject()(implicit override val messagesApi: MessagesApi,
                                      components: ControllerComponents,
                                      clock: Clock,
                                      entityAccess: JvmEntityAccess,
                                      playConfiguration: play.api.Configuration,
                                      env: play.api.Environment,
                                      webJarAssets: controllers.WebJarAssets)
    extends AbstractController(components)
    with I18nSupport {

  private lazy val localDatabaseWebWorkerResultCache: Result =
    Ok(s"""
          |importScripts("${JavascriptFiles.Assets.webworkerDeps.urlPath}");
          |importScripts("${JavascriptFiles.Assets.clientApp.urlPath}");
          |LocalDatabaseWebWorkerScript.run();
      """.stripMargin).as("application/javascript")
  def localDatabaseWebWorker = Action(_ => localDatabaseWebWorkerResultCache)

  private def serviceWorkerResultFunc(): Result = {
    val jsFileTemplate = ResourceFiles.read("/serviceWorker.template.js")
    val scriptPathsJs = JavascriptFiles.Assets.all.map(asset => s"'${asset.urlPath}'").mkString(", ")
    val cacheNameSuffix = {
      val hasher = Hashing.murmur3_128().newHasher()
      for (asset <- JavascriptFiles.Assets.all) {
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
}

object JavascriptFiles {
  private object Assets {
    private val clientAppProjectName: String = "client"
    private val webworkerDepsProjectName: String = "webworker-client-deps"

    val clientApp: Asset =
      firstExistingVersionedAsset(s"$clientAppProjectName-opt.js", s"$clientAppProjectName-fastopt.js")
    val clientAppDeps: Asset =
      firstExistingVersionedAsset(s"$clientAppProjectName-jsdeps.min.js", s"$clientAppProjectName-jsdeps.js")
    val webworkerDeps: Asset =
      firstExistingVersionedAsset(
        s"$webworkerDepsProjectName-jsdeps.min.js",
        s"$webworkerDepsProjectName-jsdeps.js")

    val all: Seq[Asset] = Seq(
      clientApp,
      clientAppDeps,
      webworkerDeps,
      WebJarAsset("metisMenu/1.1.3/metisMenu.min.css"),
      WebJarAsset("font-awesome/4.6.2/css/font-awesome.min.css"),
      WebJarAsset("font-awesome/4.6.2/fonts/fontawesome-webfont.woff2?v=4.6.2"),
      WebJarAsset("font-awesome/4.6.2/fonts/fontawesome-webfont.woff?v=4.6.2 0"),
      WebJarAsset("font-awesome/4.6.2/fonts/fontawesome-webfont.ttf?v=4.6.2"),
      VersionedAsset("images/favicon192x192.png"),
      VersionedAsset("lib/bootstrap/css/bootstrap.min.css"),
      VersionedAsset("lib/fontello/css/fontello.css"),
      UnversionedAsset("lib/fontello/font/fontello.woff2?49985636"),
      VersionedAsset("bower_components/startbootstrap-sb-admin-2/dist/css/sb-admin-2.css"),
      VersionedAsset("stylesheets/main.min.css"),
      VersionedAsset("bower_components/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js"),
      DynamicAsset(routes.JavascriptFiles.localDatabaseWebWorker)
    )

    private def firstExistingVersionedAsset(filenames: String*): Asset =
      VersionedAsset(filenames.find(name => ResourceFiles.exists(s"/public/$name")).get)

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
      override def urlPath = routes.Assets.versioned(relativePath).path()
    }
    case class UnversionedAsset(relativePath: String) extends ResourceAsset(relativePath) {
      override def urlPath = s"/assets/$relativePath"
    }
    case class WebJarAsset(relativePath: String) extends Asset {
      override def maybeLocalResource = None
      override def urlPath = routes.WebJarAssets.at(relativePath).path()
    }
    case class DynamicAsset(call: Call) extends Asset {
      override def maybeLocalResource = None
      override def urlPath = call.path()
    }
  }
}
