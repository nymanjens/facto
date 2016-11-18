package models.accounting.money

import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

import com.google.common.collect.{ImmutableMultiset, Multiset}
import common.Clock
import common.CollectionUtils.toListMap
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.JodaToSqlDateMapper
import models.manager.{Entity, SlickEntityManager, EntityTable, ImmutableEntityManager}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

import SlickExchangeRateMeasurementManager.{ExchangeRateMeasurements, tableName}

final class SlickExchangeRateMeasurementManager extends ImmutableEntityManager[ExchangeRateMeasurement, ExchangeRateMeasurements](
  SlickEntityManager.create[ExchangeRateMeasurement, ExchangeRateMeasurements](
    tag => new ExchangeRateMeasurements(tag),
    tableName = tableName
  )) with ExchangeRateMeasurement.Manager {

  type AdditionListener = ExchangeRateMeasurement => Unit
  @volatile private var listeners: Vector[AdditionListener] = Vector.empty

  override def add(measurement: ExchangeRateMeasurement): ExchangeRateMeasurement = {
    val added = super.add(measurement)

    // Call addition listeners
    for (additionListener <- listeners) {
      additionListener(measurement)
    }
    added
  }

  override def fetchAll(currency: Currency): Seq[ExchangeRateMeasurement] = {
    dbRun(
      newQuery
        .filter(_.foreignCurrencyCode === currency.code)
        .sortBy(m => m.date)
    ).toList
  }

  def addListener(measurementAddedListener: AdditionListener): Unit = {
    listeners :+= measurementAddedListener
  }
}

object SlickExchangeRateMeasurementManager {
  private val tableName: String = "EXCHANGE_RATE_MEASUREMENT"

  final class ExchangeRateMeasurements(tag: SlickTag) extends EntityTable[ExchangeRateMeasurement](tag, tableName) {
    def date = column[DateTime]("date")
    def foreignCurrencyCode = column[String]("foreignCurrencyCode")
    def ratioReferenceToForeignCurrency = column[Double]("ratioReferenceToForeignCurrency")

    override def * = (date, foreignCurrencyCode, ratioReferenceToForeignCurrency, id.?) <> (ExchangeRateMeasurement.tupled, ExchangeRateMeasurement.unapply)
  }
}
