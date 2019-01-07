package app.api

import com.google.inject.AbstractModule

final class ScalaJsApiModule extends AbstractModule {
  override def configure() = {
    bind(classOf[ScalaJsApiServerFactory])
  }
}
