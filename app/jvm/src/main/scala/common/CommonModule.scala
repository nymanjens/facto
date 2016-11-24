package common

import com.google.inject._
import tools.ApplicationStartHook

final class CommonModule extends AbstractModule {

  override def configure() = {
    bind(classOf[I18n]).to(classOf[PlayI18n]).asEagerSingleton
  }
}
