package controllers

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.inject.Inject
import common.money.Currency
import common.time.{Clock, TimeUtils}
import models._
import models.accounting._
import models.accounting.config.{Account, Config, Template}
import models.modification.EntityModification
import models.modificationhandler.EntityModificationHandler
import models.money.ExchangeRateMeasurement
import models.user.{SlickUserManager, User}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.collection.immutable.Seq

final class ExternalApi @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  clock: Clock,
                                  entityModificationHandler: EntityModificationHandler,
                                  playConfiguration: play.api.Configuration,
                                  accountingConfig: Config,
                                  userManager: SlickUserManager,
                                  entityAccess: SlickEntityAccess)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def addTransactionFromTemplate(templateCode: String, applicationSecret: String) = Action {
    implicit request =>
      validateApplicationSecret(applicationSecret)

      val template = accountingConfig.templateWithCode(templateCode)
      val issuer = getOrCreateRobotUser()

      val groupAddition = EntityModification.createAddWithRandomId(TransactionGroup(createdDate = clock.now))
      val transactionAdditions =
        toTransactions(template, transactionGroup = groupAddition.entity, issuer)
          .map(EntityModification.createAddWithRandomId(_))
      entityModificationHandler.persistEntityModifications(
        groupAddition +: transactionAdditions
      )

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
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }

  private def getOrCreateRobotUser()(implicit request: Request[_]): User = {
    val loginName = "robot"
    def hash(s: String) = Hashing.sha512().hashString(s, Charsets.UTF_8).toString

    userManager.findByLoginName(loginName) match {
      case Some(user) => user
      case None =>
        val user = SlickUserManager.createUser(
          loginName = loginName,
          password = hash(clock.now.toString),
          name = Messages("facto.robot")
        )
        entityModificationHandler.persistEntityModifications(EntityModification.createAddWithRandomId(user))
        userManager.findByLoginName(loginName).get
    }
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
}
