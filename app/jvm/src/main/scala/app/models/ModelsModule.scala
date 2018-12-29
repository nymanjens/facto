package app.models

import com.google.inject.AbstractModule
import app.models.access.AppEntityAccess
import app.models.access.JvmEntityAccess

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bindSingleton(classOf[AppEntityAccess], classOf[JvmEntityAccess])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
