package controllers

import com.google.inject.Inject
import common.money.Currency
import common.money.MoneyWithGeneralCurrency
import common.time.Clock
import common.time.TimeUtils
import app.models.Entity
import app.models.access.DbQueryImplicits._
import app.models.access.JvmEntityAccess
import app.models.access.ModelField
import app.models.accounting._
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.config.Template
import app.models.modification.EntityModification
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import app.models.user.Users
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.collection.immutable.Seq

final class ExternalApi @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  playConfiguration: play.api.Configuration,
                                  accountingConfig: Config,
                                  entityAccess: JvmEntityAccess)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def healthCheck = Action { implicit request =>
    entityAccess.checkConsistentCaches()
    Ok("OK")
  }

  def addTransactionFromTemplate(templateCode: String, applicationSecret: String) = Action {
    implicit request =>
      validateApplicationSecret(applicationSecret)

      implicit val issuer = Users.getOrCreateRobotUser()
      val template = accountingConfig.templateWithCode(templateCode)

      val groupAddition = EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
      val transactionAdditions =
        toTransactions(template, transactionGroup = groupAddition.entity, issuer)
          .map(EntityModification.createAddWithRandomId(_))
      entityAccess.persistEntityModifications(
        groupAddition +: transactionAdditions
      )

      Ok("OK")
  }

  def addExchangeRateMeasurement(dateString: String,
                                 foreignCurrencyCode: String,
                                 ratioReferenceToForeignCurrency: Double,
                                 applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    implicit val user = Users.getOrCreateRobotUser()
    val date = TimeUtils.parseDateString(dateString)
    require(Currency.of(foreignCurrencyCode).isForeign)

    entityAccess.persistEntityModifications(
      EntityModification.createAddWithRandomId(
        ExchangeRateMeasurement(
          date = date,
          foreignCurrencyCode = foreignCurrencyCode,
          ratioReferenceToForeignCurrency = ratioReferenceToForeignCurrency)))
    Ok("OK")
  }

  def listBalanceCorrections(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val resultBuilder = StringBuilder.newBuilder
    resultBuilder.append("OK\n\n")

    for (moneyReservoir <- accountingConfig.moneyReservoirs(includeHidden = true)) {
      val balanceCorrections = findBalanceCorrections(moneyReservoir)
      if (balanceCorrections.nonEmpty) {
        val extraIfHidden = if (moneyReservoir.hidden) " [HIDDEN]" else ""
        resultBuilder.append(s"${moneyReservoir.name} (${moneyReservoir.code})${extraIfHidden}:\n")
        for (correction <- balanceCorrections.reverse) {
          resultBuilder.append(
            s"  ${correction.balanceCheck.checkDate.toLocalDate}: " +
              s"${correction.expectedAmount} -> ${correction.balanceCheck.balance} " +
              s"(${forceSign(correction.balanceCheck.balance - correction.expectedAmount)})\n")
        }
        resultBuilder.append("\n")
      }
    }

    Ok(resultBuilder.toString().trim())
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }

  private def toTransactions(template: Template,
                             transactionGroup: TransactionGroup,
                             issuer: User): Seq[Transaction] = {
    def checkNotEmpty(s: String): String = {
      require(!s.isEmpty)
      s
    }
    val groupPartial = template.toPartial(Account.nullInstance)
    for (partial <- groupPartial.transactions)
      yield
        Transaction(
          transactionGroupId = transactionGroup.id,
          issuerId = issuer.id,
          beneficiaryAccountCode = checkNotEmpty(partial.beneficiary.get.code),
          moneyReservoirCode = checkNotEmpty(partial.moneyReservoir.get.code),
          categoryCode = checkNotEmpty(partial.category.get.code),
          description = checkNotEmpty(partial.description),
          flowInCents = partial.flowInCents,
          detailDescription = partial.detailDescription,
          tags = partial.tags,
          createdDate = clock.now,
          transactionDate = clock.now,
          consumedDate = clock.now
        )
  }

  private case class BalanceCorrection(balanceCheck: BalanceCheck, expectedAmount: MoneyWithGeneralCurrency)
  private def findBalanceCorrections(moneyReservoir: MoneyReservoir): List[BalanceCorrection] = {
    val balanceChecks =
      entityAccess
        .newQuerySync[BalanceCheck]()
        .filter(ModelField.BalanceCheck.moneyReservoirCode === moneyReservoir.code)
        .data()
    val transactions =
      entityAccess
        .newQuerySync[Transaction]()
        .filter(ModelField.Transaction.moneyReservoirCode === moneyReservoir.code)
        .data()

    // merge the two
    val mergedRows = (transactions ++ balanceChecks).sortBy {
      case trans: Transaction => (trans.transactionDate, trans.createdDate)
      case bc: BalanceCheck   => (bc.checkDate, bc.createdDate)
    }

    def findMismatches(nextRows: List[Entity],
                       currentBalance: MoneyWithGeneralCurrency): List[BalanceCorrection] =
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

  private def forceSign(money: MoneyWithGeneralCurrency): String = {
    val sign = if (money.cents > 0) "+" else "-"
    val positiveAmount = if (money.cents > 0) money else -money
    s"$sign $positiveAmount"
  }
}
