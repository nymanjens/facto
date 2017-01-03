package common.testing

import models.User
import models.accounting.config.Account.SummaryTotalRowDef
import models.accounting.config._

import scala.collection.immutable.{ListMap, Seq}

object TestObjects {

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
    userLoginName = Some("testUserA"),
    defaultCashReservoirCode = Some("CASH_A"),
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
    userLoginName = Some("testUserB"),
    defaultCashReservoirCode = Some("CASH_B"),
    defaultElectronicReservoirCode = "CARD_B",
    categories = Seq(testCategoryB))
  def testAccount: Account = testAccountA


  val testReservoirCashCommon = MoneyReservoir(
    code = "CASH_COMMON",
    name = "Cash Common",
    shorterName = "Cash Common",
    owner = testAccountCommon,
    hidden = false)
  val testReservoirCardCommon = MoneyReservoir(
    code = "CARD_COMMON",
    name = "Card Common",
    shorterName = "Card Common",
    owner = testAccountCommon,
    hidden = false)
  val testReservoirCashA = MoneyReservoir(
    code = "CASH_A",
    name = "Cash A",
    shorterName = "Cash A",
    owner = testAccountA,
    hidden = false)
  val testReservoirCardA = MoneyReservoir(
    code = "CARD_A",
    name = "Card A",
    shorterName = "Card A",
    owner = testAccountA,
    hidden = false)
  val testReservoirCashB = MoneyReservoir(
    code = "CASH_B",
    name = "Cash B",
    shorterName = "Cash B",
    owner = testAccountB,
    hidden = false)
  val testReservoirCardB = MoneyReservoir(
    code = "CARD_B",
    name = "Card B",
    shorterName = "Card B",
    owner = testAccountB,
    hidden = false)
  val testReservoirHidden = MoneyReservoir(
    code = "HIDDEN",
    name = "Card Hidden",
    shorterName = "Card Hidden",
    owner = testAccountB,
    hidden = true)
  val testReservoirCashGbp = MoneyReservoir(
    code = "CASH_GBP",
    name = "Cash GBP",
    shorterName = "Cash GBP",
    owner = testAccountA,
    hidden = true,
    currencyCode = Some("GBP"))
  def testReservoirOfAccountA: MoneyReservoir = testReservoirCashA
  def testReservoirOfAccountB: MoneyReservoir = testReservoirCashB
  def testReservoir: MoneyReservoir = testReservoirCashCommon
  def otherTestReservoir: MoneyReservoir = testReservoirCardCommon

  val testTemplate: Template = Template(
    code = "new-endowment",
    name = "New Endowment",
    placement = Set(Template.Placement.EndowmentsView),
    zeroSum = true,
    iconClass = "fa-plus-square",
    transactions = Seq(
      Template.Transaction(
        beneficiaryCodeTpl = Some("ACC_COMMON"),
        moneyReservoirCodeTpl = Some(""),
        categoryCodeTpl = Some("CAT_A"),
        descriptionTpl = "Endowment for ${account.longName}"),
      Template.Transaction(
        beneficiaryCodeTpl = Some("${account.code}"),
        moneyReservoirCodeTpl = Some(""),
        categoryCodeTpl = Some("CAT_A"),
        descriptionTpl = "Endowment for ${account.longName}")))

  val testConstants = Constants(
    commonAccount = testAccountCommon,
    accountingCategory = testCategoryA,
    endowmentCategory = testCategoryB,
    liquidationDescription = "Liquidation",
    zoneId = "Europe/Brussels")

  implicit val testAccountingConfig: Config = Config(
    accounts = createListMap(
      "ACC_COMMON" -> testAccountCommon,
      "ACC_A" -> testAccountA,
      "ACC_B" -> testAccountB),
    categories = createListMap(
      "CAT_B" -> testCategoryB,
      "CAT_A" -> testCategoryA),
    moneyReservoirsMap = createListMap(
      "CASH_COMMON" -> testReservoirCashCommon,
      "CARD_COMMON" -> testReservoirCardCommon,
      "CASH_A" -> testReservoirCashA,
      "CARD_A" -> testReservoirCardA,
      "CASH_B" -> testReservoirCashB,
      "CARD_B" -> testReservoirCardB,
      "HIDDEN" -> testReservoirHidden,
      "CASH_GBP" -> testReservoirCashGbp),
    templates = Seq(testTemplate),
    constants = testConstants)

  private def createListMap[K, V](elems: (K, V)*): ListMap[K, V] = {
    val resultBuilder = ListMap.newBuilder[K, V]
    elems.foreach(resultBuilder += _)
    resultBuilder.result
  }

  def testUserA: User = User(
    loginName = "testUserA",
    passwordHash = "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User A",
    idOption = Option(918273)
  )
  val testUserB: User = User(
    loginName = "testUserB",
    passwordHash = "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User B",
    idOption = Option(918274)
  )
  def testUser: User = testUserA
}
