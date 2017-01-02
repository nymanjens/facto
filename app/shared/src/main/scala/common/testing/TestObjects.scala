package common.testing

import com.google.inject._
import models.accounting.config.Account.SummaryTotalRowDef
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.{SlickEntityAccess, SlickUserManager, User}

import scala.collection.immutable.Seq

object TestObjects {

  implicit lazy val accountingConfig: Config = {
    Guice.createInjector(new FactoTestModule).getInstance(classOf[Config])
  }

  val testCategoryA: Category = Category(code = "CAT_A", name = "Category A")
  val testCategoryB: Category = Category(code = "CAT_B", name = "Category B", helpText = "b-help")
  def testCategory: Category = testCategoryA

  val testAccountCommon: Account = Account(
    code = "ACC_COMMON",
    longName = "Account Common",
    shorterName = "Acc.Common",
    veryShortName = "Common",
    defaultElectronicReservoirCode = "CARD_COMMON",
    categories = Seq(testCategoryA, testCategoryB))
  val testAccountA: Account = Account(
    code = "ACC_A",
    longName = "Account A",
    shorterName = "Acc.A",
    veryShortName = "A",
    userLoginName = Option("a"),
    defaultCashReservoirCode = Option("CASH_A"),
    defaultElectronicReservoirCode = "CARD_A",
    categories = Seq(testCategoryA, testCategoryB),
    summaryTotalRows = Seq(
      SummaryTotalRowDef(
        rowTitleHtml = "<b>Total</b>",
        categoriesToIgnore = Set()),
      SummaryTotalRowDef(
        rowTitleHtml = "<b>Total</b> (without catA)",
        categoriesToIgnore = Set(testCategoryA))))
  val testAccountB: Account = Account(
    code = "ACC_B",
    longName = "Account B",
    shorterName = "Acc.B",
    veryShortName = "B",
    userLoginName = Option("b"),
    defaultCashReservoirCode = Option("CASH_B"),
    defaultElectronicReservoirCode = "CARD_B",
    categories = Seq(testCategoryB))
  def testAccount: Account = testAccountA

  def testReservoirOfAccountA: MoneyReservoir = accountingConfig.moneyReservoir("CASH_A")
  def testReservoirOfAccountB: MoneyReservoir = accountingConfig.moneyReservoir("CASH_B")
  def testReservoir: MoneyReservoir = accountingConfig.moneyReservoir("CASH_COMMON")
  def otherTestReservoir: MoneyReservoir = accountingConfig.moneyReservoir("CARD_COMMON")

  def testUser: User = User(
    loginName = "testUser",
    passwordHash = "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User"
  )
}
