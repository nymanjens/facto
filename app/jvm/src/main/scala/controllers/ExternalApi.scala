package controllers

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.inject.Inject
import common.time.{Clock, TimeUtils}
import models.accounting._
import models.accounting.config.{Account, Config}
import models.accounting.money.{Currency, ExchangeRateMeasurement}

import scala.collection.immutable.Seq
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import common.cache.CacheRegistry
import models._
import controllers.accounting.Views
import controllers.helpers.{AuthenticatedAction, ControllerHelperCache}
import controllers.Application.Forms.{AddUserData, ChangePasswordData}

final class ExternalApi @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  viewsController: Views,
                                  playConfiguration: play.api.Configuration,
                                  accountingConfig: Config,
                                  userManager: SlickUserManager,
                                  entityAccess: SlickEntityAccess)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def doCacheManagement(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)
    CacheRegistry.doMaintenanceAndVerifyConsistency()
    Ok("OK")
  }

  /** Warms up caches by rendering the most used views. */
  def warmUpCaches(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val admin: User = userManager.findByLoginName("admin").get
    val actions: Seq[AuthenticatedAction[AnyContent]] = Seq(
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

  def addTransactionFromTemplate(templateCode: String, applicationSecret: String) = Action {
    implicit request =>
      validateApplicationSecret(applicationSecret)

      val template = accountingConfig.templateWithCode(templateCode)
      val partial = template.toPartial(Account.nullInstance)
      val issuer = getOrCreateRobotUser()

      // Add group
      val group = entityAccess.transactionGroupManager.add(TransactionGroup(createdDate = clock.now))

      // Add transactions
      for (transPartial <- partial.transactions) {
        val transaction = transactionPartialToTransaction(transPartial, group, issuer)
        entityAccess.transactionManager.add(transaction)
      }

      // Add log
      entityAccess.updateLogManager.addLog(issuer, UpdateLog.AddNew, group)

      Ok("OK")
  }

  def addExchangeRateMeasurement(dateString: String,
                                 foreignCurrencyCode: String,
                                 ratioReferenceToForeignCurrency: Double,
                                 applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val date = TimeUtils.parseDateString(dateString)
    require(Currency.of(foreignCurrencyCode).isForeign)

    entityAccess.exchangeRateMeasurementManager.add(
      ExchangeRateMeasurement(
        date = date,
        foreignCurrencyCode = foreignCurrencyCode,
        ratioReferenceToForeignCurrency = ratioReferenceToForeignCurrency))
    Ok("OK")
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String) = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.crypto.secret")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }

  private def getOrCreateRobotUser()(implicit request: Request[_]): User = {
    val loginName = "robot"
    def hash(s: String) = Hashing.sha512().hashString(s, Charsets.UTF_8).toString()

    userManager.findByLoginName(loginName) match {
      case Some(user) => user
      case None =>
        val user = SlickUserManager.createUser(
          loginName = loginName,
          password = hash(clock.now.toString),
          name = Messages("facto.robot")
        )
        userManager.add(user)
    }
  }

  private def transactionPartialToTransaction(partial: Transaction.Partial,
                                              transactionGroup: TransactionGroup,
                                              issuer: User): Transaction = {
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
      createdDate = clock.now,
      transactionDate = clock.now,
      consumedDate = clock.now
    )
  }
}
