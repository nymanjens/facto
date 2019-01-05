package app.common.testing

import app.api.ScalaJsApiModule
import app.models.ModelsModule
import app.models.access.JvmEntityAccess
import app.models.accounting.config.ConfigModule
import com.google.inject._
import hydro.common.testing.FakeClock
import hydro.common.time._
import hydro.common.PlayI18n
import hydro.common.testing.FakePlayI18n
import hydro.common.I18n
import hydro.models.access.EntityAccess
import hydro.models.access.JvmEntityAccessBase

final class TestModule extends AbstractModule {

  override def configure() = {
    install(new ConfigModule)
    install(new ModelsModule)
    install(new ScalaJsApiModule)
    bindSingleton(classOf[Clock], classOf[FakeClock])
    bindSingleton(classOf[PlayI18n], classOf[FakePlayI18n])
    bind(classOf[I18n]).to(classOf[PlayI18n])

    bind(classOf[EntityAccess]).to(classOf[JvmEntityAccess])
    bind(classOf[JvmEntityAccessBase]).to(classOf[JvmEntityAccess])
  }

  @Provides()
  private[testing] def playConfiguration(): play.api.Configuration = {
    play.api.Configuration.from(
      Map(
        "app.accounting.configYamlFilePath" -> "/test-accounting-config.yml"
      ))
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
