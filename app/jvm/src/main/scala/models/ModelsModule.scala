package models

import com.google.inject.AbstractModule

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bindSingleton(classOf[EntityAccess], classOf[SlickEntityAccess])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
