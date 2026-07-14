package app.flux.stores.entries.factories

import hydro.common.GuavaReplacement.DoubleMath.roundToLong
import app.common.accounting.ComplexQueryFilter
import app.common.money.Currency
import app.common.money.CurrencyValueManager
import app.common.money.ReferenceMoney
import app.common.time.DatedMonth
import app.flux.stores.entries.factories.ChartStoreFactory.LinePoints
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory.ExchangeRateGains
import app.flux.stores.entries.factories.SummaryInflationGainsStoreFactory.InflationGains
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.accounting.Transaction
import app.models.modification.EntityTypes
import app.models.money.ExchangeRateMeasurement
import hydro.common.time.Clock
import hydro.common.time.LocalDateTime
import hydro.common.ScalaUtils
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.flux.stores.CombiningStateStore
import hydro.flux.stores.CombiningStateStore3
import hydro.flux.stores.FixedStateStore
import hydro.flux.stores.MappingStore
import hydro.flux.stores.StateStore
import hydro.flux.stores.StoreFactory
import hydro.models.access.DbQuery
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Store factory that calculates the points on a graph based on a search query. This includes exchange rate gains.
 */
final class ChartStoreFactory(implicit
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    complexQueryFilter: ComplexQueryFilter,
    summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
    summaryInflationGainsStoreFactory: SummaryInflationGainsStoreFactory,
    currencyValueManager: CurrencyValueManager,
    clock: Clock,
) extends StoreFactory {
  // **************** Public API **************** //
  def get(query: String, correctForInflation: Boolean): Store = {
    getCachedOrCreate(Input(query, correctForInflation))
  }

  // **************** Implementation of base class methods and types **************** //
  /* override */
  protected case class Input(
      queryString: String,
      correctForInflation: Boolean,
  )

  /* override */
  type Store = StateStore[LinePoints]

  override protected def createNew(input: Input): Store = {
    if (input.queryString.startsWith("@exchangeRate:")) {
      val foreignCurrencyCode = input.queryString.stripPrefix("@exchangeRate:")
      MappingStore.map[Option[LinePoints], LinePoints](
        new ExchangeRateLinePointsStore(foreignCurrencyCode),
        _.getOrElse(LinePoints.empty),
      )
    } else {
      val filterFromQuery = complexQueryFilter.fromQuery(input.queryString)
      new StoreWithComplexQuery(
        filterFromQuery,
        new ChartStoreFromEntities(filterFromQuery, correctForInflation = input.correctForInflation),
        summaryExchangeRateGainsStoreFactory.get(correctForInflation = input.correctForInflation),
        if (input.correctForInflation) summaryInflationGainsStoreFactory.get()
        else FixedStateStore(Some(InflationGains.empty)),
      )
    }
  }

  // **************** Private inner types **************** //
  final class StoreWithComplexQuery(
      filterFromQuery: DbQuery.Filter[Transaction],
      chartStoreFromEntities: ChartStoreFromEntities,
      summaryExchangeRateGainsStore: StateStore[Option[ExchangeRateGains]],
      summaryInflationGainsStore: StateStore[Option[InflationGains]],
  ) extends CombiningStateStore3[
        Option[LinePoints],
        Option[ExchangeRateGains],
        Option[InflationGains],
        LinePoints,
      ](
        chartStoreFromEntities,
        summaryExchangeRateGainsStore,
        summaryInflationGainsStore,
      ) {

    override protected def combineStoreStates(
        maybeChartFromEntities: Option[LinePoints],
        maybeExchangeRateGains: Option[ExchangeRateGains],
        maybeInflationGains: Option[InflationGains],
    ): LinePoints = {
      (for {
        chartFromEntities <- maybeChartFromEntities
        exchangeRateGains <- maybeExchangeRateGains
        inflationGains <- maybeInflationGains
      } yield {
        val exchangeRateGainsPoints = LinePoints(
          exchangeRateGains.monthToGains
            // Future months are irrelevant for exchange rate gains because they are not yet known
            .filterKeys(_ <= DatedMonth.current)
            .map { case (month, gainsForMonth) =>
              month -> gainsForMonth.reservoirToGains.map { case (reservoir, gains) =>
                // Dummy transaction to be filterable
                val dummyTransaction = Transaction(
                  transactionGroupId = EntityModification.generateRandomId(),
                  issuerId = EntityModification.generateRandomId(),
                  beneficiaryAccountCode = reservoir.owner.code,
                  moneyReservoirCode = reservoir.code,
                  categoryCode = "Exchange",
                  description = "Exchange rate gains",
                  detailDescription = "",
                  tags = Seq(),
                  attachments = Seq(),
                  flowInCents = gains.cents,
                  createdDate = month.middleTime,
                  transactionDate = month.middleTime,
                  consumedDate = month.middleTime,
                  idOption = Some(EntityModification.generateRandomId()),
                )
                if (filterFromQuery(dummyTransaction)) gains else ReferenceMoney(0)
              }.sum
            }
            // Omit leading zeros
            .toVector
            .sortBy(_._1)
            .dropWhile(_._2.isZero)
            .toMap
        )

        val inflationGainsPoints = LinePoints(
          inflationGains.monthToGains
            // Future months are irrelevant for inflation gains because they are not yet known
            .filterKeys(_ <= DatedMonth.current)
            .map { case (month, gainsForMonth) =>
              month -> gainsForMonth.reservoirToGains.map { case (reservoir, gains) =>
                // Dummy transaction to be filterable
                val dummyTransaction = Transaction(
                  transactionGroupId = EntityModification.generateRandomId(),
                  issuerId = EntityModification.generateRandomId(),
                  beneficiaryAccountCode = reservoir.owner.code,
                  moneyReservoirCode = reservoir.code,
                  categoryCode = "Inflation",
                  flowInCents = gains.cents,
                  description = "Inflation gains",
                  detailDescription = "",
                  tags = Seq(),
                  attachments = Seq(),
                  createdDate = month.middleTime,
                  transactionDate = month.middleTime,
                  consumedDate = month.middleTime,
                  idOption = Some(EntityModification.generateRandomId()),
                )
                if (filterFromQuery(dummyTransaction)) gains else ReferenceMoney(0)
              }.sum
            }
            // Omit leading zeros
            .toVector
            .sortBy(_._1)
            .dropWhile(_._2.isZero)
            .toMap
        )

        chartFromEntities ++ exchangeRateGainsPoints ++ inflationGainsPoints
      }) getOrElse LinePoints.empty
    }
  }

  private final class ExchangeRateLinePointsStore(foreignCurrencyCode: String)
      extends AsyncEntityDerivedStateStore[LinePoints] {
    override protected def calculateState(): Future[LinePoints] = async {
      val currency = Currency.General(foreignCurrencyCode)
      LinePoints(
        DatedMonth
          .monthsInClosedRange(currencyValueManager.getStartOfMeasurementsMonth(currency), DatedMonth.current)
          .map(month =>
            month -> toReferenceMoney(
              currencyValueManager
                .getRatioSecondToFirstCurrency(currency, Currency.default, month.endTime)
            )
          )
          .toMap
      )
    }

    override protected def modificationImpactsState(
        entityModification: EntityModification,
        state: LinePoints,
    ): Boolean = entityModification.entityType == ExchangeRateMeasurement.Type

    private def toReferenceMoney(double: Double): ReferenceMoney = {
      ReferenceMoney(roundToLong(100 * double))
    }
  }

  private final class ChartStoreFromEntities(
      filterFromQuery: DbQuery.Filter[Transaction],
      correctForInflation: Boolean,
  ) extends AsyncEntityDerivedStateStore[LinePoints] {
    override protected def calculateState(): Future[LinePoints] = async {
      val transactions: Seq[Transaction] =
        await(entityAccess.newQuery[Transaction]().filter(filterFromQuery).data())

      LinePoints(
        transactions
          .groupBy(t => DatedMonth.containing(t.consumedDate))
          // Don't show future transactions in charts because the data will likely paint an incomplete picture
          .filterKeys(_ <= DatedMonth.current)
          .mapValues(
            _.map(_.flow.exchangedForReferenceCurrency(correctForInflation = correctForInflation)).sum
          )
      )
    }

    override protected def modificationImpactsState(
        entityModification: EntityModification,
        state: LinePoints,
    ): Boolean = true
  }
}
object ChartStoreFactory {
  case class LinePoints(points: Map[DatedMonth, ReferenceMoney]) {
    def ++(that: LinePoints): LinePoints = {
      val newMonths = this.points.keySet ++ that.points.keySet
      LinePoints(
        newMonths
          .map(month =>
            (
              month,
              this.points.getOrElse(month, ReferenceMoney(0)) +
                that.points.getOrElse(month, ReferenceMoney(0)),
            )
          )
          .toMap
      )
    }
  }
  object LinePoints {
    val empty: LinePoints = LinePoints(points = Map())
  }
}
