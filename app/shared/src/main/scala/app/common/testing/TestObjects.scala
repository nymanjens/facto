package app.common.testing

import java.time.Instant
import java.time.Month
import java.time.Month._

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApi.UpdateToken
import app.api.ScalaJsApi.UserPrototype
import app.common.accounting.ChartSpec
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.accounting.config._
import app.models.accounting.config.Account.SummaryTotalRowDef
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.common.OrderToken
import hydro.common.time.LocalDateTimes
import hydro.common.time.LocalDateTimes.createDateTime
import hydro.models.modification.EntityModification
import hydro.models.UpdatableEntity.LastUpdateTime

import scala.collection.immutable.ListMap
import scala.collection.immutable.Seq
import scala.collection.immutable.Set
import scala.util.Random

object TestObjects {

  def testCategoryA: Category = Category(code = "CAT_A", name = "Category A")
  def testCategoryB: Category = Category(code = "CAT_B", name = "Category B", helpText = "b-help")
  def testCategoryC: Category = Category(code = "CAT_C", name = "Category C")
  def testCategory: Category = testCategoryA

  def testAccountCommon: Account = Account(
    code = "ACC_COMMON",
    longName = "Account Common",
    shorterName = "Acc.Common",
    veryShortName = "Common",
    defaultElectronicReservoirCode = "CARD_COMMON",
    categories = Seq(testCategoryA, testCategoryB),
    summaryTotalRows = Seq(SummaryTotalRowDef(rowTitleHtml = "<b>Total</b>", categoriesToIgnore = Set())),
  )
  def testAccountA: Account = Account(
    code = "ACC_A",
    longName = "Account A",
    shorterName = "Acc.A",
    veryShortName = "A",
    userLoginName = Some("testUserA"),
    defaultCashReservoirCode = Some("CASH_A"),
    defaultElectronicReservoirCode = "CARD_A",
    categories = Seq(testCategoryA, testCategoryB),
    summaryTotalRows = Seq(
      SummaryTotalRowDef(rowTitleHtml = "<b>Total</b>", categoriesToIgnore = Set()),
      SummaryTotalRowDef(
        rowTitleHtml = "<b>Total</b> (without catA)",
        categoriesToIgnore = Set(testCategoryA),
      ),
    ),
  )
  def testAccountB: Account = Account(
    code = "ACC_B",
    longName = "Account B",
    shorterName = "Acc.B",
    veryShortName = "B",
    userLoginName = Some("testUserB"),
    defaultCashReservoirCode = Some("CASH_B"),
    defaultElectronicReservoirCode = "CARD_B",
    categories = Seq(testCategoryB),
    summaryTotalRows = Seq(SummaryTotalRowDef(rowTitleHtml = "<b>Total</b>", categoriesToIgnore = Set())),
  )
  def testAccountC: Account = Account(
    code = "ACC_C",
    longName = "Account C",
    shorterName = "Acc.C",
    veryShortName = "C",
    userLoginName = Some("testUserC"),
    defaultCashReservoirCode = Some("CASH_C"),
    defaultElectronicReservoirCode = "CARD_C",
    categories = Seq(testCategoryC),
    summaryTotalRows = Seq(SummaryTotalRowDef(rowTitleHtml = "<b>Total</b>", categoriesToIgnore = Set())),
  )
  def testAccount: Account = testAccountA

  def testReservoirCashCommon =
    MoneyReservoir(
      code = "CASH_COMMON",
      name = "Cash Common",
      shorterName = "Cash Common",
      owner = testAccountCommon,
      hidden = false,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirCardCommon =
    MoneyReservoir(
      code = "CARD_COMMON",
      name = "Card Common",
      shorterName = "Card Common",
      owner = testAccountCommon,
      hidden = false,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirCashA =
    MoneyReservoir(
      code = "CASH_A",
      name = "Cash A",
      shorterName = "Cash A",
      owner = testAccountA,
      hidden = false,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirCardA =
    MoneyReservoir(
      code = "CARD_A",
      name = "Card A",
      shorterName = "Card A",
      owner = testAccountA,
      hidden = false,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirCashB =
    MoneyReservoir(
      code = "CASH_B",
      name = "Cash B",
      shorterName = "Cash B",
      owner = testAccountB,
      hidden = false,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirCardB =
    MoneyReservoir(
      code = "CARD_B",
      name = "Card B",
      shorterName = "Card B",
      owner = testAccountB,
      hidden = false,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirHidden =
    MoneyReservoir(
      code = "HIDDEN",
      name = "Card Hidden",
      shorterName = "Card Hidden",
      owner = testAccountB,
      hidden = true,
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirCashGbp =
    MoneyReservoir(
      code = "CASH_GBP",
      name = "Cash GBP",
      shorterName = "Cash GBP",
      owner = testAccountA,
      hidden = true,
      currencyCode = Some("GBP"),
      assumeThisFollowsInflationUntilNextBalanceCheck = false,
    )
  def testReservoirOfAccountA: MoneyReservoir = testReservoirCashA
  def testReservoirOfAccountB: MoneyReservoir = testReservoirCashB
  def testReservoir: MoneyReservoir = testReservoirCashCommon
  def otherTestReservoir: MoneyReservoir = testReservoirCardCommon

  def testTemplate: Template = Template(
    code = "new-endowment",
    name = "New Endowment",
    placement = Set(Template.Placement.EndowmentsView),
    zeroSum = true,
    iconClass = "fa-plus-square",
    transactions = Seq(
      Template.Transaction(
        beneficiaryCodeTpl = "ACC_COMMON",
        moneyReservoirCodeTpl = "",
        categoryCode = "CAT_A",
        description = "Bakery",
        detailDescription = "These are the details.",
      ),
      Template.Transaction(
        beneficiaryCodeTpl = "${account.code}",
        moneyReservoirCodeTpl = "",
        categoryCode = "CAT_A",
        description = "Bakery",
        detailDescription = "These are the details.",
      ),
    ),
  )

  def testConstants = Constants(
    commonAccount = testAccountCommon,
    accountingCategory = testCategoryA,
    endowmentCategory = testCategoryB,
    liquidationDescription = "Liquidation",
    zoneId = "Europe/Brussels",
    supportInflationCorrections = true,
    firstMonthOfYear = Month.JANUARY,
  )

  implicit val testAccountingConfig: Config = Config(
    accounts =
      createListMap("ACC_COMMON" -> testAccountCommon, "ACC_A" -> testAccountA, "ACC_B" -> testAccountB),
    categories = createListMap("CAT_B" -> testCategoryB, "CAT_A" -> testCategoryA, "CAT_C" -> testCategoryC),
    moneyReservoirsMap = createListMap(
      "CASH_COMMON" -> testReservoirCashCommon,
      "CARD_COMMON" -> testReservoirCardCommon,
      "CASH_A" -> testReservoirCashA,
      "CARD_A" -> testReservoirCardA,
      "CASH_B" -> testReservoirCashB,
      "CARD_B" -> testReservoirCardB,
      "HIDDEN" -> testReservoirHidden,
      "CASH_GBP" -> testReservoirCashGbp,
    ),
    templates = Seq(testTemplate),
    predefinedCharts = Seq(
      PredefinedChart(
        name = "Account A's transactions",
        chartSpec = ChartSpec(
          Seq(
            ChartSpec.Line(
              name = "Transactions",
              query = "beneficiary:'Account A'",
            )
          ),
          correctForInflation = false,
          aggregationPeriod = ChartSpec.AggregationPeriod.Month,
        ),
      )
    ),
    constants = testConstants,
  )

  def createAccountingConfig(firstMonthOfYear: Month = Month.JANUARY): Config = {
    testAccountingConfig.copy(constants = testConstants.copy(firstMonthOfYear = firstMonthOfYear))
  }

  private def createListMap[K, V](elems: (K, V)*): ListMap[K, V] = {
    val resultBuilder = ListMap.newBuilder[K, V]
    elems.foreach(resultBuilder += _)
    resultBuilder.result
  }

  def testLastUpdateTime = LastUpdateTime.allFieldsUpdated(testInstant)

  def testUserA: User = User(
    loginName = "testUserA",
    passwordHash =
      "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User A",
    isAdmin = false,
    expandCashFlowTablesByDefault = true,
    expandLiquidationTablesByDefault = true,
    idOption = Option(918273),
    lastUpdateTime = testLastUpdateTime,
  )
  def testUserB: User = User(
    loginName = "testUserB",
    passwordHash =
      "be196838736ddfd0007dd8b2e8f46f22d440d4c5959925cb49135abc9cdb01e84961aa43dd0ddb6ee59975eb649280d9f44088840af37451828a6412b9b574fc",
    // = sha512("pw")
    name = "Test User B",
    isAdmin = false,
    expandCashFlowTablesByDefault = true,
    expandLiquidationTablesByDefault = true,
    idOption = Option(918274),
    lastUpdateTime = testLastUpdateTime,
  )
  def testUser: User = testUserA
  def testUserRedacted: User = testUser.copy(passwordHash = "<redacted>")

  def testUserPrototype =
    UserPrototype.create(
      id = testUser.id,
      loginName = testUser.loginName,
      plainTextPassword = "dlkfjasfd",
      name = testUser.name,
      isAdmin = testUser.isAdmin,
    )

  def orderTokenA: OrderToken = OrderToken.middleBetween(None, Some(OrderToken.middle))
  def orderTokenB: OrderToken = OrderToken.middleBetween(Some(OrderToken.middle), None)
  def orderTokenC: OrderToken = OrderToken.middleBetween(Some(orderTokenB), None)
  def orderTokenD: OrderToken = OrderToken.middleBetween(Some(orderTokenC), None)
  def orderTokenE: OrderToken = OrderToken.middleBetween(Some(orderTokenD), None)
  def testOrderToken: OrderToken = orderTokenC

  def testDate: LocalDateTime = LocalDateTimes.createDateTime(2008, MARCH, 13)
  def testInstantA: Instant = Instant.ofEpochMilli(999000001)
  def testInstantB: Instant = Instant.ofEpochMilli(999000002)
  def testInstantC: Instant = Instant.ofEpochMilli(999000003)
  def testInstantD: Instant = Instant.ofEpochMilli(999000004)
  def testInstant: Instant = testInstantA
  def testUpdateToken: UpdateToken = s"123782:12378"

  def testTransactionGroupWithId: TransactionGroup =
    TransactionGroup(createdDate = testDate, idOption = Some(129874444))
  def testTransactionWithIdA: Transaction = Transaction(
    transactionGroupId = testTransactionGroupWithId.id,
    issuerId = testUserA.id,
    beneficiaryAccountCode = testAccountA.code,
    moneyReservoirCode = testReservoir.code,
    categoryCode = testCategoryA.code,
    description = "Test description",
    flowInCents = -123,
    createdDate = testDate,
    transactionDate = testDate,
    consumedDate = testDate,
    idOption = Some(721309875),
  )
  def testTransactionWithIdB: Transaction = Transaction(
    transactionGroupId = testTransactionGroupWithId.id,
    issuerId = testUserB.id,
    beneficiaryAccountCode = testAccountB.code,
    moneyReservoirCode = testReservoir.code,
    categoryCode = testCategoryB.code,
    description = "Test description",
    flowInCents = -123,
    createdDate = testDate,
    transactionDate = testDate,
    consumedDate = testDate,
    idOption = Some(4371098),
  )
  def testTransactionWithId: Transaction = testTransactionWithIdA
  def testBalanceCheckWithId: BalanceCheck =
    BalanceCheck(
      issuerId = testUser.id,
      moneyReservoirCode = testReservoir.code,
      balanceInCents = 38746,
      createdDate = testDate,
      checkDate = testDate,
      idOption = Some(873865333),
    )
  def testExchangeRateMeasurementWithId: ExchangeRateMeasurement =
    ExchangeRateMeasurement(
      date = testDate,
      foreignCurrencyCode = "GBP",
      ratioReferenceToForeignCurrency = 1.234,
      idOption = Some(764785511),
    )

  def testModificationA: EntityModification = EntityModification.Add(testTransactionWithIdA)
  def testModificationB: EntityModification = EntityModification.Add(testTransactionWithIdB)
  def testModification: EntityModification = testModificationA

  def testGetInitialDataResponse: GetInitialDataResponse = GetInitialDataResponse(
    accountingConfig = testAccountingConfig,
    user = testUserA,
    allUsers = Seq(testUserA, testUserB),
    i18nMessages = Map(),
    ratioReferenceToForeignCurrency = Map(),
    nextUpdateToken = "1234:5678",
  )

  private val unsetDouble: Double = -387461.19
  def createTransaction(
      id: Long = -1,
      groupId: Long = -1,
      issuer: User = testUserA,
      beneficiary: Account = testAccountA,
      reservoir: MoneyReservoir = null,
      year: Int = 2012,
      month: Month = MARCH,
      day: Int = 25,
      category: Category = testCategory,
      description: String = "some description",
      flow: Double = unsetDouble,
      detailDescription: String = "some detail description",
      tags: Seq[String] = Seq("some-tag"),
  ): Transaction = {
    testTransactionWithId.copy(
      idOption = Some(if (id == -1) EntityModification.generateRandomId() else id),
      transactionGroupId = if (groupId == -1) EntityModification.generateRandomId() else groupId,
      issuerId = issuer.id,
      beneficiaryAccountCode = beneficiary.code,
      moneyReservoirCode = Option(reservoir).map(_.code) getOrElse "",
      categoryCode = category.code,
      description = description,
      flowInCents = if (flow == unsetDouble) Random.nextLong() % 10000 else (flow * 100).toLong,
      detailDescription = detailDescription,
      tags = tags,
      createdDate = createDateTime(year, month, day),
      transactionDate = createDateTime(year, month, day),
      consumedDate = createDateTime(year, month, day),
    )
  }

  def createBalanceCheck(
      id: Long = -1,
      issuer: User = testUserA,
      reservoir: MoneyReservoir = testReservoir,
      year: Int = 2012,
      month: Month = MARCH,
      day: Int = 25,
      balance: Double = unsetDouble,
  ): BalanceCheck = {
    BalanceCheck(
      idOption = Some(if (id == -1) EntityModification.generateRandomId() else id),
      moneyReservoirCode = reservoir.code,
      balanceInCents = if (balance == unsetDouble) Random.nextLong() % 10000 else (balance * 100).toLong,
      issuerId = issuer.id,
      createdDate = createDateTime(year, month, day),
      checkDate = createDateTime(year, month, day),
    )
  }

  def createExchangeRateMeasurement(
      id: Long = -1,
      year: Int = 2012,
      month: Month = MARCH,
      day: Int = 25,
      foreignCurrencyCode: String = "GBP",
      ratio: Double = unsetDouble,
  ): ExchangeRateMeasurement = {
    ExchangeRateMeasurement(
      idOption = Some(if (id == -1) EntityModification.generateRandomId() else id),
      date = createDateTime(year, month, day),
      foreignCurrencyCode = foreignCurrencyCode,
      ratioReferenceToForeignCurrency = if (ratio == unsetDouble) Random.nextDouble() else ratio,
    )
  }
  def createTemplate(
      code: String = null,
      name: String = null,
      placement: Set[Template.Placement] = Set(),
      onlyShowForUserLoginNames: Option[Set[String]] = None,
      zeroSum: Boolean = false,
      iconClass: String = "",
      transactions: Seq[Template.Transaction],
  ): Template = {
    val randomString = EntityModification.generateRandomId().toString
    Template(
      code = Option(code) getOrElse randomString,
      name = Option(name) getOrElse randomString,
      placement = placement,
      onlyShowForUserLoginNames = onlyShowForUserLoginNames,
      zeroSum = zeroSum,
      iconClass = iconClass,
      transactions = transactions,
    )
  }
  def createTemplateTransaction(
      beneficiaryCodeTpl: String = "ACC_A",
      moneyReservoirCodeTpl: String = "CASH_A",
      categoryCode: String = "CAT_A",
      description: String = "Some description",
      flowInCents: Long = 129837,
      detailDescription: String = "",
      tags: Seq[String] = Seq(),
  ): Template.Transaction = {
    Template.Transaction(
      beneficiaryCodeTpl = beneficiaryCodeTpl,
      moneyReservoirCodeTpl = moneyReservoirCodeTpl,
      categoryCode = categoryCode,
      description = description,
      flowInCents = flowInCents,
      detailDescription = detailDescription,
      tags = tags,
    )
  }
}
