package models.accounting.money

import com.google.common.collect.{ImmutableMultiset, Multiset}
import common.Clock
import common.CollectionUtils.toListMap
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.JodaToSqlDateMapper
import models.manager.{Entity, EntityManager, EntityTable, ImmutableEntityManager}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

/** ExchangeRateMeasurement entities are immutable. */
case class ExchangeRateMeasurement(date: DateTime = Clock.now,
                                   private val foreignCurrencyCode: String,
                                   ratioReferenceToForeignCurrency: Double,
                                   idOption: Option[Long] = None) extends Entity[ExchangeRateMeasurement] {
  require(!foreignCurrencyCode.isEmpty)
  require(ratioReferenceToForeignCurrency > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def foreignCurrency: Currency = Currency.of(foreignCurrencyCode)
}

class ExchangeRateMeasurements(tag: SlickTag) extends EntityTable[ExchangeRateMeasurement](tag, ExchangeRateMeasurements.tableName) {
  def date = column[DateTime]("date")
  def foreignCurrencyCode = column[String]("foreignCurrencyCode")
  def ratioReferenceToForeignCurrency = column[Double]("ratioReferenceToForeignCurrency")

  override def * = (date, foreignCurrencyCode, ratioReferenceToForeignCurrency, id.?) <> (ExchangeRateMeasurement.tupled, ExchangeRateMeasurement.unapply)
}

object ExchangeRateMeasurements extends ImmutableEntityManager[ExchangeRateMeasurement, ExchangeRateMeasurements](
  EntityManager.create[ExchangeRateMeasurement, ExchangeRateMeasurements](
    tag => new ExchangeRateMeasurements(tag), tableName = "EXCHANGE_RATE_MEASUREMENT")) {

  def addListener(measurementAddedListener: ExchangeRateMeasurement => Unit): Unit = {
    ???
  }

  def fetchAll(currency: Currency): Seq[ExchangeRateMeasurement] = {
    dbRun(
      newQuery
        .filter(_.foreignCurrencyCode === currency.code)
        .sortBy(m => m.date)
    ).toList
  }
}
