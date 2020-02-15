package app.api

import com.google.inject.AbstractModule
import hydro.api.EntityPermissions

final class ApiModule extends AbstractModule {
  override def configure() = {
    bind(classOf[ScalaJsApiServerFactory])
    bind(classOf[EntityPermissions]).toInstance(EntityPermissions.DefaultImpl)
  }
}
