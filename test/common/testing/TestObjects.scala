package common.testing

import play.api.test.FakeApplication
import play.api.test.Helpers._
import org.joda.time.DateTime

import common.Clock
import models.{User, Users}
import models.accounting.config.{MoneyReservoir, Account, Category, Config}
import models.accounting.{Transaction, Transactions, TransactionGroups, TransactionGroup, BalanceCheck, BalanceChecks, Money}

object TestObjects {

  def testAccountA: Account = Config.accounts("ACC_A")
  def testAccountB: Account = Config.accounts("ACC_B")
  def testAccount: Account = testAccountA

  def testCategoryA: Category = Config.categories("CAT_A")
  def testCategoryB: Category = Config.categories("CAT_B")
  def testCategory: Category = testCategoryA

  def testReservoirOfAccountA: MoneyReservoir = Config.moneyReservoir("CASH_A")
  def testReservoirOfAccountB: MoneyReservoir = Config.moneyReservoir("CASH_B")
  def testReservoir: MoneyReservoir = Config.moneyReservoir("CASH_COMMON")
  def otherTestReservoir: MoneyReservoir = Config.moneyReservoir("CARD_COMMON")

  def testUser: User = {
    val loginName = "testUser"
    Users.findByLoginName(loginName) match {
      case Some(user) => user
      case None =>
        Users.add(Users.newWithUnhashedPw(loginName = loginName, password = "tu", name = "Test User"))
    }
  }
}
