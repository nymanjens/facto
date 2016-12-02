package common

import com.google.inject._
import tools.ApplicationStartHook
import common.time.{Clock, JvmClock}

final class CommonModule extends AbstractModule {

  override def configure() = {
    bind(classOf[I18n]).to(classOf[PlayI18n]).asEagerSingleton
    bind(classOf[Clock]).to(classOf[JvmClock])
  }
}
