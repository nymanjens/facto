package common

import com.google.inject._
import tools.ApplicationStartHook
import common.time.{Clock, JvmClock}

final class CommonModule extends AbstractModule {

  override def configure() = {
    bindSingleton(classOf[I18n], classOf[PlayI18n])
    bind(classOf[Clock]).to(classOf[JvmClock])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
