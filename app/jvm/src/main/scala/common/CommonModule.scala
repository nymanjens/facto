package common

import com.google.inject._
import common.time.Clock
import common.time.JvmClock

final class CommonModule extends AbstractModule {

  override def configure() = {
    bindSingleton(classOf[PlayI18n], classOf[PlayI18n.Impl])
    bind(classOf[I18n]).to(classOf[PlayI18n])

    bind(classOf[Clock]).to(classOf[JvmClock])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
