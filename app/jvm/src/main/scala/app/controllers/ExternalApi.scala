package app.controllers

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization

import java.net.URLDecoder
import java.time.LocalTime
import app.common.accounting.ComplexQueryFilter
import app.common.money.Currency
import app.common.money.MoneyWithGeneralCurrency
import app.controllers.ExternalApi.JsonSerializableMoneyReservoir.JsonSerializableBalanceCorrection
import app.controllers.ExternalApi.JsonSerializableMoneyReservoir
import app.controllers.ExternalApi.JsonSerializableTransaction
import app.models.access.AppDbQuerySorting
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.accounting._
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.config.Template
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import app.models.user.Users
import com.google.inject.Inject
import hydro.common.ValidatingYamlParser.ParsableValue.DoubleValue
import hydro.common.ValidatingYamlParser.ParsableValue.ListParsableValue
import hydro.common.ValidatingYamlParser.ParsableValue.LocalDateTimeValue
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Optional
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Required
import hydro.common.ValidatingYamlParser.ParsableValue.MapParsableValue.StringMap
import hydro.common.ValidatingYamlParser.ParsableValue.StringValue
import hydro.common.time.Clock
import hydro.common.time.LocalDateTime
import hydro.common.time.TimeUtils
import hydro.common.ScalaUtils.ifThenOption
import hydro.common.Tags
import hydro.common.ValidatingYamlParser
import hydro.models.modification.EntityModification
import hydro.models.Entity
import hydro.models.access.DbQueryImplicits._
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.collection.immutable.Seq
import scala.collection.immutable.SortedMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration

final class ExternalApi @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    playConfiguration: play.api.Configuration,
    accountingConfig: Config,
    entityAccess: JvmEntityAccess,
) extends AbstractController(components)
    with I18nSupport {

  // ********** actions: read only ********** //
  def listReservoirs(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val moneyReservoirs: Seq[JsonSerializableMoneyReservoir] =
      for (reservoir <- accountingConfig.moneyReservoirs(includeHidden = true)) yield {
        val balance = getCurrentBalance(reservoir)
        val latestBalanceCheck = getLastBalanceCheck(reservoir)
        val balanceCorrections = findBalanceCorrections(reservoir)

        JsonSerializableMoneyReservoir(
          code = reservoir.code,
          name = reservoir.name,
          ownerAccountCode = reservoir.owner.code,
          hidden = reservoir.hidden,
          currencyCode = balance.currency.code,
          balance = balance.toDouble,
          lastBalanceCheckDate = latestBalanceCheck.map(_.checkDate.toLocalDate.toString).orNull,
          balanceCorrections = balanceCorrections.map(bc =>
            JsonSerializableBalanceCorrection(
              date = bc.balanceCheck.checkDate.toLocalDate.toString,
              expectedBalance = bc.expectedAmount.toDouble,
              checkedBalance = bc.balanceCheck.balance.toDouble,
            )
          ),
        )
      }

    Ok(
      JsonSerializableMoneyReservoir.toJson(
        moneyReservoirs.sortBy(r => (-r.balanceCorrections.size, r.hidden))
      )
    )
  }

  def searchTransactions(encodedSearchString: String, applicationSecret: String) = Action {
    implicit request =>
      validateApplicationSecret(applicationSecret)

      val searchString = decodeUrlEncodedString(encodedSearchString)
      val searchQuery = (new ComplexQueryFilter).fromQuery(searchString)
      val matchedTransactions =
        entityAccess
          .newQuerySync[Transaction]()
          .filter(searchQuery)
          .sort(AppDbQuerySorting.Transaction.deterministicallyByConsumedDate.reversed)
          .data()

      val serializableTransactions = matchedTransactions.map { transaction =>
        JsonSerializableTransaction(
          transactionGroupId = transaction.transactionGroupId,
          beneficiaryAccountCode = transaction.beneficiaryAccountCode,
          moneyReservoirCode = transaction.moneyReservoirCode,
          categoryCode = transaction.categoryCode,
          description = transaction.description,
          flowInCents = transaction.flowInCents,
          tags = transaction.tags,
          createdDate = transaction.createdDate.toLocalDate.toString,
          transactionDate = transaction.transactionDate.toLocalDate.toString,
          consumedDate = transaction.consumedDate.toLocalDate.toString,
        )
      }

      Ok(JsonSerializableTransaction.toJson(serializableTransactions))
  }

  // ********** actions: mutating ********** //
  def addTransactionFromTemplate(templateCode: String, applicationSecret: String) = Action {
    implicit request =>
      validateApplicationSecret(applicationSecret)

      persistTransactionGroup(transactionsWithoutIdFunc =
        (group, issuer) =>
          toTransactions(accountingConfig.templateWithCode(templateCode), transactionGroup = group, issuer)
      )

      Ok("OK")
  }

  def addTransaction(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)
    require(
      request.body.asText.isDefined,
      s"This method requires POST text data (text/plain) in YAML format, but got ${request.body}",
    )

    persistTransactionGroup(transactionsWithoutIdFunc =
      (group, issuer) =>
        ValidatingYamlParser.parse(
          request.body.asText.get,
          TransactionGroupParsableValue(transactionGroupId = group.id, issuerId = issuer.id),
        )
    )

    Ok("OK")
  }

  def deleteTransaction(applicationSecret: String, transactionGroupId: Long) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)
    implicit val issuer = Users.getOrCreateRobotUser()

    val group: TransactionGroup = entityAccess.newQuerySync[TransactionGroup]().findById(transactionGroupId)
    val transactions: Seq[Transaction] = Await.result(group.transactions, Duration.Inf)

    val transactionDeletions = transactions map (EntityModification.createRemove(_))
    val groupDeletion = EntityModification.createRemove(group)
    entityAccess.persistEntityModifications(transactionDeletions :+ groupDeletion)

    Ok("OK")
  }

  def addExchangeRateMeasurement(
      dateString: String,
      foreignCurrencyCode: String,
      ratioReferenceToForeignCurrency: Double,
      applicationSecret: String,
  ) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    implicit val user = Users.getOrCreateRobotUser()
    val date = TimeUtils.parseDateString(dateString)
    require(Currency.of(foreignCurrencyCode).isForeign)

    entityAccess.persistEntityModifications(
      EntityModification.createAddWithRandomId(
        ExchangeRateMeasurement(
          date = date,
          foreignCurrencyCode = foreignCurrencyCode,
          ratioReferenceToForeignCurrency = ratioReferenceToForeignCurrency,
        )
      )
    )
    Ok("OK")
  }

  def addTagRefactor(
      encodedSearchString: String,
      tagToAdd: String,
      dryOrWetRun: String,
      applicationSecret: String,
  ) = Action { implicit request =>
    require(Tags.isValidTag(tagToAdd))

    applyRefactor(
      encodedSearchString = encodedSearchString,
      updateDescription = s"Adding tag '$tagToAdd'",
      updateToApplyToMatchingTransactions = t => {
        if (t.tags contains tagToAdd) t
        else t.copy(tags = t.tags :+ tagToAdd)
      },
      dryOrWetRun = dryOrWetRun,
      applicationSecret = applicationSecret,
    )
  }

  def refactorTransactionCategory(
      encodedSearchString: String,
      newCategoryCode: String,
      dryOrWetRun: String,
      applicationSecret: String,
  ) = Action { implicit request =>
    require(accountingConfig.categories.contains(newCategoryCode), s"Unrecognized category: $newCategoryCode")

    applyRefactor(
      encodedSearchString = encodedSearchString,
      updateDescription = s"Changing category to $newCategoryCode",
      updateToApplyToMatchingTransactions = _.copy(categoryCode = newCategoryCode),
      dryOrWetRun = dryOrWetRun,
      applicationSecret = applicationSecret,
    )
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'",
    )
  }

  def applyRefactor(
      encodedSearchString: String,
      updateDescription: String,
      updateToApplyToMatchingTransactions: Transaction => Transaction,
      dryOrWetRun: String,
      applicationSecret: String,
  ) = {
    validateApplicationSecret(applicationSecret)

    val searchString = decodeUrlEncodedString(encodedSearchString)
    val searchQuery = (new ComplexQueryFilter).fromQuery(searchString)
    val matchedTransactions =
      entityAccess
        .newQuerySync[Transaction]()
        .filter(searchQuery)
        .sort(AppDbQuerySorting.Transaction.deterministicallyByCreateDate.reversed)
        .data()
    val transactionsToEdit = matchedTransactions.filterNot(t => updateToApplyToMatchingTransactions(t) == t)

    implicit val issuer = Users.getOrCreateRobotUser()
    val modifications = (
      for {
        transactionGroupId <- transactionsToEdit.map(_.transactionGroupId).distinct
        (transaction, newId) <- zipWithIncrementingId(
          entityAccess
            .newQuerySync[Transaction]()
            .filter(ModelFields.Transaction.transactionGroupId === transactionGroupId)
            .sort(AppDbQuerySorting.Transaction.deterministicallyByCreateDate)
            .data()
        )
      } yield Seq(
        EntityModification.createRemove(transaction),
        EntityModification.createAddWithId(
          newId,
          (
            if (transactionsToEdit contains transaction) updateToApplyToMatchingTransactions(transaction)
            else transaction
          ).copy(idOption = None),
        ),
      )
    ).flatten

    dryOrWetRun match {
      case "dry" => // Do nothing
      case "wet" => entityAccess.persistEntityModifications(modifications)
    }

    Ok(
      s"""searchString                           = $searchString
         |update                                 = $updateDescription
         |#transactions that match searchString  = ${matchedTransactions.size}
         |#transactions that will be edited      = ${transactionsToEdit.size}
         |#entity modifications                  = ${modifications.size}
         |
         |Run                                    = $dryOrWetRun
         |
         |Transactions that will be edited:
         |
         |${transactionsToEdit.map(t => s"- ${t.description} (${t.category}, ${t.tags})\n").mkString("")}
         |""".stripMargin
    )
  }

  private def persistTransactionGroup(
      transactionsWithoutIdFunc: (TransactionGroup, User) => Seq[Transaction]
  ) = {
    implicit val issuer = Users.getOrCreateRobotUser()

    val groupAddition = EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))

    val transactionsWithoutId: Seq[Transaction] = transactionsWithoutIdFunc(groupAddition.entity, issuer)
    val transactionAdditions =
      for ((transactionWithoutId, id) <- zipWithIncrementingId(transactionsWithoutId)) yield {
        EntityModification.createAddWithId(id, transactionWithoutId)
      }

    entityAccess.persistEntityModifications(
      groupAddition +: transactionAdditions
    )
  }

  private def toTransactions(
      template: Template,
      transactionGroup: TransactionGroup,
      issuer: User,
  ): Seq[Transaction] = {
    def checkNotEmpty(s: String): String = {
      require(!s.isEmpty)
      s
    }

    val groupPartial = template.toPartial(Account.nullInstance)
    for (partial <- groupPartial.transactions)
      yield Transaction(
        transactionGroupId = transactionGroup.id,
        issuerId = issuer.id,
        beneficiaryAccountCode = checkNotEmpty(partial.beneficiary.get.code),
        moneyReservoirCode = partial.moneyReservoir.get.code,
        categoryCode = checkNotEmpty(partial.category.get.code),
        description = checkNotEmpty(partial.description),
        flowInCents = partial.flowInCents,
        detailDescription = partial.detailDescription,
        tags = partial.tags,
        attachments = partial.attachments,
        createdDate = clock.now,
        transactionDate = clock.now,
        consumedDate = clock.now,
      )
  }

  private case class BalanceCorrection(balanceCheck: BalanceCheck, expectedAmount: MoneyWithGeneralCurrency)
  private def findBalanceCorrections(moneyReservoir: MoneyReservoir): List[BalanceCorrection] = {
    val balanceChecks =
      entityAccess
        .newQuerySync[BalanceCheck]()
        .filter(ModelFields.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
        .data()
    val transactions =
      entityAccess
        .newQuerySync[Transaction]()
        .filter(ModelFields.Transaction.moneyReservoirCode === moneyReservoir.code)
        .data()

    // merge the two
    val mergedRows = (transactions ++ balanceChecks).sortBy {
      case trans: Transaction => (trans.transactionDate, trans.createdDate)
      case bc: BalanceCheck   => (bc.checkDate, bc.createdDate)
    }

    def findMismatches(
        nextRows: List[Entity],
        currentBalance: MoneyWithGeneralCurrency,
    ): List[BalanceCorrection] =
      (nextRows: @unchecked) match {
        case (trans: Transaction) :: rest =>
          findMismatches(rest, currentBalance = currentBalance + trans.flow)
        case (bc: BalanceCheck) :: rest =>
          if (bc.balance == currentBalance) {
            findMismatches(rest, bc.balance)
          } else {
            BalanceCorrection(bc, expectedAmount = currentBalance) :: findMismatches(rest, bc.balance)
          }
        case Nil =>
          Nil
      }

    findMismatches(mergedRows.toList, currentBalance = MoneyWithGeneralCurrency(0, moneyReservoir.currency))
  }

  private def getLastBalanceCheck(moneyReservoir: MoneyReservoir): Option[BalanceCheck] = {
    entityAccess
      .newQuerySync[BalanceCheck]()
      .sort(AppDbQuerySorting.BalanceCheck.deterministicallyByCheckDate.reversed)
      .findOne(ModelFields.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
  }
  private def getCurrentBalance(moneyReservoir: MoneyReservoir): MoneyWithGeneralCurrency = {
    val balanceChecks =
      entityAccess
        .newQuerySync[BalanceCheck]()
        .filter(ModelFields.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
        .data()
    val transactions =
      entityAccess
        .newQuerySync[Transaction]()
        .filter(ModelFields.Transaction.moneyReservoirCode === moneyReservoir.code)
        .data()

    // merge the two
    val mergedRows = (transactions ++ balanceChecks).sortBy {
      case trans: Transaction => (trans.transactionDate, trans.createdDate)
      case bc: BalanceCheck   => (bc.checkDate, bc.createdDate)
    }

    def nextBalance(
        nextRows: List[Entity],
        currentBalance: MoneyWithGeneralCurrency,
    ): MoneyWithGeneralCurrency =
      (nextRows: @unchecked) match {
        case (trans: Transaction) :: rest =>
          nextBalance(rest, currentBalance = currentBalance + trans.flow)
        case (bc: BalanceCheck) :: rest =>
          nextBalance(rest, bc.balance)
        case Nil => currentBalance
      }

    nextBalance(mergedRows.toList, currentBalance = MoneyWithGeneralCurrency(0, moneyReservoir.currency))
  }

  private def forceSign(money: MoneyWithGeneralCurrency): String = {
    val sign = if (money.cents > 0) "+" else "-"
    val positiveAmount = if (money.cents > 0) money else -money
    s"$sign $positiveAmount"
  }

  private def zipWithIncrementingId[E](entities: Seq[E]): Seq[(E, Long)] = {
    val ids = {
      val start = EntityModification.generateRandomId()
      val end = start + entities.size
      start until end
    }
    entities zip ids
  }

  private def decodeUrlEncodedString(s: String): String = {
    URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
  }

  private case class TransactionGroupParsableValue(transactionGroupId: Long, issuerId: Long)
      extends MapParsableValue[Seq[Transaction]] {
    private val transactionParsableValue = TransactionParsableValue(transactionGroupId, issuerId)

    override val supportedKeyValuePairs = Map(
      "transactions" -> Required(ListParsableValue(transactionParsableValue)(_.description))
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      map.required[Seq[Transaction]]("transactions")
    }
  }

  private case class TransactionParsableValue(transactionGroupId: Long, issuerId: Long)
      extends MapParsableValue[Transaction] {
    override val supportedKeyValuePairs = Map(
      "beneficiaryCode" -> Required(StringValue),
      "moneyReservoirCode" -> Required(StringValue),
      "categoryCode" -> Required(StringValue),
      "description" -> Required(StringValue),
      "flowAsFloat" -> Required(DoubleValue),
      "detailDescription" -> Optional(StringValue),
      "tags" -> Optional(ListParsableValue(StringValue)(s => s)),
      "transactionDate" -> Optional(LocalDateTimeValue),
      "consumedDate" -> Optional(LocalDateTimeValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      val today = LocalDateTime.of(clock.now.toLocalDate, LocalTime.MIN)

      Transaction(
        transactionGroupId = transactionGroupId,
        issuerId = issuerId,
        beneficiaryAccountCode = map.required[String]("beneficiaryCode"),
        moneyReservoirCode = map.required[String]("moneyReservoirCode"),
        categoryCode = map.required[String]("categoryCode"),
        flowInCents = (map.required[Double]("flowAsFloat") * 100).round,
        description = map.required[String]("description"),
        detailDescription = map.optional("detailDescription", defaultValue = ""),
        tags = map.optional("tags", defaultValue = Seq()),
        attachments = Seq(),
        createdDate = clock.now,
        transactionDate = map.optional("transactionDate", defaultValue = today),
        consumedDate = map.optional("consumedDate", defaultValue = today),
      )
    }

    override def additionalValidationErrors(transaction: Transaction): Seq[String] = {
      def maybeError(
          fieldName: String,
          getter: Transaction => String,
          options: Traversable[String],
      ): Option[String] = {
        ifThenOption(!options.toSet.contains(getter(transaction))) {
          s"$fieldName=${getter(transaction)} was not recognized (valid values: ${options}",
        }
      }

      val allMoneyReservoirs =
        accountingConfig.moneyReservoirs(includeHidden = true, includeNullReservoir = true).map(_.code)

      Seq(
        maybeError("beneficiaryAccountCode", _.beneficiaryAccountCode, accountingConfig.accounts.keySet),
        maybeError("moneyReservoirCode", _.moneyReservoirCode, allMoneyReservoirs),
        maybeError("categoryCode", _.categoryCode, accountingConfig.categories.keySet),
        ifThenOption(!transaction.tags.forall(Tags.isValidTag)) {
          s"Invalid tag in transaction.tags: ${transaction.tags}"
        },
      ).flatten
    }
  }
}
object ExternalApi {
  case class JsonSerializableMoneyReservoir(
      code: String,
      name: String,
      ownerAccountCode: String,
      hidden: Boolean,
      currencyCode: String,
      balance: Double,
      lastBalanceCheckDate: String,
      balanceCorrections: Seq[JsonSerializableBalanceCorrection],
  )
  object JsonSerializableMoneyReservoir {
    def toJson(reservoirs: Seq[JsonSerializableMoneyReservoir]): String = {
      implicit val formats = DefaultFormats
      Serialization.writePretty(reservoirs)
    }
    case class JsonSerializableBalanceCorrection(
        date: String,
        expectedBalance: Double,
        checkedBalance: Double,
    )
  }

  case class JsonSerializableTransaction(
      transactionGroupId: Long,
      beneficiaryAccountCode: String,
      moneyReservoirCode: String,
      categoryCode: String,
      description: String,
      flowInCents: Long,
      tags: Seq[String],
      createdDate: String,
      transactionDate: String,
      consumedDate: String,
  )
  object JsonSerializableTransaction {
    def toJson(transactions: Seq[JsonSerializableTransaction]): String = {
      implicit val formats = DefaultFormats
      Serialization.writePretty(transactions)
    }
  }
}
