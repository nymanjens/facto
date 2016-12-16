package common.testing

import com.google.inject._
import collection.immutable.Seq
import play.api.test.FakeApplication
import play.api.test.Helpers._

import common.time._
import common._
import models.accounting.config.{MoneyReservoir, Account, Category, Config, ConfigModule}
import models.accounting.{Transaction, TransactionGroup, BalanceCheck}
import models.ModelsModule

final class FactoTestModule extends AbstractModule {

  override def configure() = {
    install(new ConfigModule)
    install(new ModelsModule)
    bindSingleton(classOf[Clock], classOf[FakeClock])
    bindSingleton(classOf[I18n], classOf[FakeI18n])
  }

  @Provides()
  private[testing] def playConfiguration(): play.api.Configuration = {
    play.api.Configuration.from(Map(
      "facto.accounting.configYamlFilePath" -> "/test-accounting-config.yml"
    ))
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
