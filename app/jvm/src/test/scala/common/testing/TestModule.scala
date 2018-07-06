package common.testing

import api.ScalaJsApiModule
import com.google.inject._
import common._
import common.time._
import models.ModelsModule
import models.accounting.config.ConfigModule

final class TestModule extends AbstractModule {

  override def configure() = {
    install(new ConfigModule)
    install(new ModelsModule)
    install(new ScalaJsApiModule)
    bindSingleton(classOf[Clock], classOf[FakeClock])
    bindSingleton(classOf[PlayI18n], classOf[FakePlayI18n])
    bind(classOf[I18n]).to(classOf[PlayI18n])
  }

  @Provides()
  private[testing] def playConfiguration(): play.api.Configuration = {
    play.api.Configuration.from(
      Map(
        "facto.accounting.configYamlFilePath" -> "/test-accounting-config.yml"
      ))
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
