package app.models

import com.google.inject.AbstractModule
import app.models.access.EntityAccess
import app.models.access.JvmEntityAccess

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bindSingleton(classOf[EntityAccess], classOf[JvmEntityAccess])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
