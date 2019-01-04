package app.controllers

import app.controllers.InternalApi.ScalaJsApiCaller
import app.controllers.JavascriptFiles.appAssets
import app.controllers.JavascriptFiles.Asset
import app.controllers.JavascriptFiles.UnversionedAsset
import app.controllers.JavascriptFiles.VersionedAsset
import app.controllers.helpers.ScalaJsApiCallerImpl
import com.google.inject.AbstractModule
import com.google.inject.Provides

import scala.collection.immutable.Seq

final class ControllersModule extends AbstractModule {

  override def configure() = {
    bind(classOf[ScalaJsApiCaller]).to(classOf[ScalaJsApiCallerImpl])
  }

  @Provides @appAssets def provideAppAssets: Seq[Asset] = Seq(
    VersionedAsset("bootstrap/dist/css/bootstrap.min.css"),
    VersionedAsset("metismenu/dist/metisMenu.min.css"),
    VersionedAsset("font-awesome/css/font-awesome.min.css"),
    UnversionedAsset("font-awesome/fonts/fontawesome-webfont.woff2?v=4.6.3"),
    UnversionedAsset("font-awesome/fonts/fontawesome-webfont.woff?v=4.6.3 0"),
    UnversionedAsset("font-awesome/fonts/fontawesome-webfont.ttf?v=4.6.3"),
    VersionedAsset("lib/fontello/css/fontello.css"),
    UnversionedAsset("lib/fontello/font/fontello.woff2?49985636"),
    VersionedAsset("startbootstrap-sb-admin-2/dist/css/sb-admin-2.css"),
    VersionedAsset("stylesheets/main.min.css"),
    VersionedAsset("jquery/dist/jquery.min.js"),
    VersionedAsset("bootstrap/dist/js/bootstrap.min.js"),
    VersionedAsset("metismenu/dist/metisMenu.min.js"),
    VersionedAsset("startbootstrap-sb-admin-2/dist/js/sb-admin-2.js"),
  )
}
