package models.accounting.money

import common.time.LocalDateTime
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.{dbRun, localDateTimeToSqlDateMapper}
import models.accounting.money.SlickExchangeRateMeasurementManager.{ExchangeRateMeasurements, tableName}
import models.manager.{EntityTable, ImmutableEntityManager, SlickEntityManager}

import scala.collection.immutable.Seq

final class SlickExchangeRateMeasurementManager
    extends ImmutableEntityManager[ExchangeRateMeasurement, ExchangeRateMeasurements](
      SlickEntityManager.create[ExchangeRateMeasurement, ExchangeRateMeasurements](
        tag => new ExchangeRateMeasurements(tag),
        tableName = tableName
      ))
    with ExchangeRateMeasurement.Manager {

  override def fetchAll(currency: Currency): Seq[ExchangeRateMeasurement] = {
    dbRun(
      newQuery
        .filter(_.foreignCurrencyCode === currency.code)
        .sortBy(m => m.date)
    ).toList
  }
}

object SlickExchangeRateMeasurementManager {
  private val tableName: String = "EXCHANGE_RATE_MEASUREMENT"

  final class ExchangeRateMeasurements(tag: SlickTag)
      extends EntityTable[ExchangeRateMeasurement](tag, tableName) {
    def date = column[LocalDateTime]("date")
    def foreignCurrencyCode = column[String]("foreignCurrencyCode")
    def ratioReferenceToForeignCurrency = column[Double]("ratioReferenceToForeignCurrency")

    override def * =
      (date, foreignCurrencyCode, ratioReferenceToForeignCurrency, id.?) <> (ExchangeRateMeasurement.tupled, ExchangeRateMeasurement.unapply)
  }
}
