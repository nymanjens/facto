package controllers.accounting

import com.google.inject.Inject
import common.time.{Clock, TimeUtils}
import controllers.helpers.accounting.GeneralEntry
import models.accounting.money.{Currency, DatedMoney, ExchangeRateManager}
import play.api.data.{FormError, Forms}
import play.api.mvc._
import play.api.i18n.{MessagesApi, Messages, I18nSupport}

import models._
import play.api.libs.json.Json
import models.accounting.TagEntity
import models.accounting.config.{Config, Template}
import controllers.helpers.AuthenticatedAction
import models.SlickUtils.dbApi._
import models.SlickUtils.{localDateTimeToSqlDateMapper, dbRun}

final class JsonApi @Inject()(implicit override val messagesApi: MessagesApi,components: ControllerComponents,
                              clock: Clock,
                              entityAccess: SlickEntityAccess,
                              playConfiguration: play.api.Configuration,
                              exchangeRateManager: ExchangeRateManager)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def filterDescriptions(beneficiaryCode: String,
                         reservoirCode: String,
                         categoryCode: String,
                         query: String) =
    AuthenticatedAction { implicit user => implicit request =>
      val descriptions = dbRun(
        entityAccess.transactionManager.newQuery
          .filter(_.beneficiaryAccountCode === beneficiaryCode)
          .filter(_.moneyReservoirCode === reservoirCode)
          .filter(_.categoryCode === categoryCode)
          .filter(_.description.toLowerCase startsWith query.toLowerCase)
          .sortBy(r => (r.createdDate.desc))
          .map(_.description)
          .take(50)).distinct
        .take(10)
      Ok(Json.toJson(descriptions))
    }

  def getAllTags = AuthenticatedAction { implicit user => implicit request =>
    val tagNames = entityAccess.tagEntityManager.fetchAll().map(_.tag.name).toSet
    Ok(Json.toJson(tagNames))
  }

  def exchangeMoney(fromCents: Long, fromCurrencyCode: String, dateString: String, toCurrencyCode: String) =
    AuthenticatedAction { implicit user => implicit request =>
      val date = TimeUtils.parseDateString(dateString)
      val fromMoney = DatedMoney(fromCents, Currency.of(fromCurrencyCode), date)
      val toMoney = fromMoney.exchangedForCurrency(Currency.of(toCurrencyCode))
      Ok(Json.toJson(toMoney.cents))
    }
}
