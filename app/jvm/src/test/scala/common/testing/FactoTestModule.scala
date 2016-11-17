package common.testing

import com.google.inject._
import collection.immutable.Seq
import play.api.test.FakeApplication
import play.api.test.Helpers._
import org.joda.time.DateTime

import common.Clock
import models.{User, Users}
import models.accounting.config.{MoneyReservoir, Account, Category, Config, ConfigModule}
import models.accounting.{Transaction, Transactions, TransactionGroups, TransactionGroup, BalanceCheck}

final class FactoTestModule extends AbstractModule {

  override def configure() = {
    install(new ConfigModule)
  }

  @Provides()
  private[testing] def playConfiguration(): play.api.Configuration = {
    play.api.Configuration.from(Map(
      "facto.accounting.configYamlFilePath" -> "/test-accounting-config.yml"
    ))
  }
}
