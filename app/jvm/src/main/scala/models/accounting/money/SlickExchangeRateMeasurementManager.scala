package models.accounting.money

import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

import com.google.common.collect.{ImmutableMultiset, Multiset}
import common.time.Clock
import common.CollectionUtils.toListMap
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.manager.{Entity, SlickEntityManager, EntityTable, ImmutableEntityManager}
import common.time.LocalDateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

import SlickExchangeRateMeasurementManager.{ExchangeRateMeasurements, tableName}

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
