package controllers

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import common.Clock
import models.accounting._
import models.accounting.config.{Account, Config}

import scala.collection.immutable.Seq
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._
import play.Play.application
import play.api.i18n.Messages

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import common.cache.CacheRegistry
import models.{Tables, Users, User}
import controllers.accounting.Views
import controllers.helpers.{ControllerHelperCache, AuthenticatedAction}
import controllers.Application.Forms.{AddUserData, ChangePasswordData}

object ExternalApi extends Controller {

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
      Views.everythingLatest,
      Views.cashFlowOfAll,
      Views.liquidationOfAll,
      Views.endowmentsOfAll,
      Views.summaryForCurrentYear()
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


  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String) = {
    val realApplicationSecret = application.configuration.getString("play.crypto.secret")
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
      flow = partial.flow,
      detailDescription = partial.detailDescription,
      tagsString = partial.tagsString,
      transactionDate = Clock.now,
      consumedDate = Clock.now)
  }
}
