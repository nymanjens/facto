package app.models

import app.models.access.AppEntityAccess
import app.models.access.JvmEntityAccess
import com.google.inject.AbstractModule
import hydro.models.access.EntityAccess

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bind(classOf[AppEntityAccess]).to(classOf[JvmEntityAccess])
    bind(classOf[EntityAccess]).to(classOf[JvmEntityAccess])
    bind(classOf[JvmEntityAccess]).asEagerSingleton
  }
}
