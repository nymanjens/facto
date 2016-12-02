package common.testing

import com.google.inject._
import collection.immutable.Seq
import play.api.test.FakeApplication
import play.api.test.Helpers._

import common.time.Clock
import models.{User, EntityAccess}
import models.accounting.config.{MoneyReservoir, Account, Category, Config, ConfigModule}
import models.accounting.{Transaction, TransactionGroup, BalanceCheck}

object TestObjects {

  implicit lazy val accountingConfig: Config = {
    Guice.createInjector(new FactoTestModule).getInstance(classOf[Config])
  }

  def testAccountA: Account = accountingConfig.accounts("ACC_A")
  def testAccountB: Account = accountingConfig.accounts("ACC_B")
  def testAccount: Account = testAccountA

  def testCategoryA: Category = accountingConfig.categories("CAT_A")
  def testCategoryB: Category = accountingConfig.categories("CAT_B")
  def testCategory: Category = testCategoryA

  def testReservoirOfAccountA: MoneyReservoir = accountingConfig.moneyReservoir("CASH_A")
  def testReservoirOfAccountB: MoneyReservoir = accountingConfig.moneyReservoir("CASH_B")
  def testReservoir: MoneyReservoir = accountingConfig.moneyReservoir("CASH_COMMON")
  def otherTestReservoir: MoneyReservoir = accountingConfig.moneyReservoir("CARD_COMMON")

  def testUser(implicit entityAccess: EntityAccess): User = {
    val loginName = "testUser"
    entityAccess.userManager.findByLoginName(loginName) match {
      case Some(user) => user
      case None =>
        entityAccess.userManager.add(
          entityAccess.userManager.newWithUnhashedPw(
            loginName = loginName,
            password = "tu",
            name = "Test User"))
    }
  }
}
