package controllers

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.inject.Inject
import common.{Clock, TimeUtils}
import models.accounting._
import models.accounting.config.{Account, Config}
import models.accounting.money.{Currency, ExchangeRateMeasurement, ExchangeRateMeasurements}

import scala.collection.immutable.Seq
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import common.cache.CacheRegistry
import models.{Tables, User, Users}
import controllers.accounting.Views
import controllers.helpers.{AuthenticatedAction, ControllerHelperCache}
import controllers.Application.Forms.{AddUserData, ChangePasswordData}

class ExternalApi @Inject()(viewsController: Views, val messagesApi: MessagesApi, configuration: play.api.Configuration)
  extends Controller with I18nSupport {

  // ********** actions ********** //
  def doCacheManagement(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)
    CacheRegistry.doMaintenanceAndVerifyConsistency()
    Ok("OK")
  }

  /** Warms up caches by rendering the most used views. */
  def warmUpCaches(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val admin: User = Users.findByLoginName("admin").get
    val actions: Seq[AuthenticatedAction] = Seq(
      viewsController.everythingLatest,
      viewsController.cashFlowOfAll,
      viewsController.liquidationOfAll,
      viewsController.endowmentsOfAll,
      viewsController.summaryForCurrentYear()
    )
    for (action <- actions) {
      action.calculateResult(admin, request)
    }

    Ok("OK")
  }

  def addTransactionFromTemplate(templateCode: String, applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val template = Config.templateWithCode(templateCode)
    val partial = template.toPartial(Account.nullInstance)
    val issuer = getOrCreateRobotUser()

    // Add group
    val group = TransactionGroups.add(TransactionGroup())

    // Add transactions
    for (transPartial <- partial.transactions) {
      val transaction = transactionPartialToTransaction(transPartial, group, issuer)
      Transactions.add(transaction)
    }

    // Add log
    UpdateLogs.addLog(issuer, UpdateLogs.AddNew, group)

    Ok("OK")
  }

  def addExchangeRateMeasurement(dateString: String,
                                 foreignCurrencyCode: String,
                                 ratioReferenceToForeignCurrency: Double,
                                 applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val date = TimeUtils.parseDateString(dateString)
    require(Currency.of(foreignCurrencyCode).isForeign)

    ExchangeRateMeasurements.add(ExchangeRateMeasurement(
      date = date,
      foreignCurrencyCode = foreignCurrencyCode,
      ratioReferenceToForeignCurrency = ratioReferenceToForeignCurrency))
    Ok("OK")
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String) = {
    val realApplicationSecret = configuration.getString("play.crypto.secret")
    require(applicationSecret == realApplicationSecret, "Invalid application secret")
  }

  def getOrCreateRobotUser(): User = {
    val loginName = "robot"
    def hash(s: String) = Hashing.sha512().hashString(s, Charsets.UTF_8).toString()

    Users.findByLoginName(loginName) match {
      case Some(user) => user
      case None =>
        val user = Users.newWithUnhashedPw(
          loginName = loginName,
          password = hash(Clock.now.toString),
          name = Messages("facto.robot")
        )
        Users.add(user)
    }
  }

  private def transactionPartialToTransaction(partial: TransactionPartial, transactionGroup: TransactionGroup, issuer: User): Transaction = {
    def checkNotEmpty(s: String): String = {
      require(!s.isEmpty)
      s
    }
    Transaction(
      transactionGroupId = transactionGroup.id,
      issuerId = issuer.id,
      beneficiaryAccountCode = checkNotEmpty(partial.beneficiary.get.code),
      moneyReservoirCode = checkNotEmpty(partial.moneyReservoir.get.code),
      categoryCode = checkNotEmpty(partial.category.get.code),
      description = checkNotEmpty(partial.description),
      flowInCents = partial.flowInCents,
      detailDescription = partial.detailDescription,
      tagsString = partial.tagsString,
      transactionDate = Clock.now,
      consumedDate = Clock.now)
  }
}
