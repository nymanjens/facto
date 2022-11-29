package app.flux.stores.entries.factories

import app.common.accounting.ComplexQueryFilter
import app.common.accounting.DateToBalanceFunction
import app.common.money.ExchangeRateManager
import app.common.money.MoneyWithGeneralCurrency
import app.common.money.ReferenceMoney
import app.common.time.DatedMonth
import app.flux.stores.entries.factories.SummaryInflationGainsStoreFactory.GainsForMonth
import app.flux.stores.entries.factories.SummaryInflationGainsStoreFactory.InflationGains
import app.flux.stores.entries.EntriesStore
import app.models.access.AppDbQuerySorting
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.common.time.LocalDateTime
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.ModelField
import hydro.models.Entity

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Store factory that calculates the monthly gains and losses made by inflation. */
final class SummaryInflationGainsStoreFactory(implicit
    entityAccess: AppJsEntityAccess,
    exchangeRateManager: ExchangeRateManager,
    accountingConfig: Config,
    complexQueryFilter: ComplexQueryFilter,
    clock: Clock,
) extends EntriesStoreFactory[InflationGains] {

  // **************** Public API ****************//
  def get(account: Account = null, year: Int = -1): Store = {
    get(Input(account = Option(account), year = if (year == -1) None else Some(year)))
  }

  // **************** Implementation of EntriesStoreFactory methods/types ****************//
  override protected def createNew(input: Input) = new Store {
    override protected def calculateState() = async {
      InflationGains.sum {
        val futures = for {
          reservoir <- accountingConfig.moneyReservoirs(includeHidden = true)
          if isRelevantReservoir(reservoir)
        } yield calculateInflationGains(reservoir)

        await(Future.sequence(futures))
      }
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) = {
      isRelevantReservoir(transaction.moneyReservoir) &&
      (input.year.isEmpty || transaction.transactionDate.getYear <= input.year.get)
    }
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = {
      isRelevantReservoir(balanceCheck.moneyReservoir) &&
      (input.year.isEmpty || balanceCheck.checkDate.getYear <= input.year.get)
    }

    // **************** Private helper methods ****************//
    private def calculateInflationGains(reservoir: MoneyReservoir): Future[InflationGains] = async {
      val oldestRelevantBalanceCheck: Option[BalanceCheck] = input.year match {
        case None => None
        case Some(year) =>
          await(
            entityAccess
              .newQuery[BalanceCheck]()
              .filter(ModelFields.BalanceCheck.moneyReservoirCode === reservoir.code)
              .filter(ModelFields.BalanceCheck.checkDate < DatedMonth.allMonthsIn(year).head.startTime)
              .sort(AppDbQuerySorting.BalanceCheck.deterministicallyByCheckDate.reversed)
              .limit(1)
              .data()
          ).headOption
      }
      val oldestBalanceDate = oldestRelevantBalanceCheck.map(_.checkDate).getOrElse(LocalDateTime.MIN)
      val initialBalance =
        oldestRelevantBalanceCheck.map(_.balance).getOrElse(MoneyWithGeneralCurrency(0, reservoir.currency))

      val balanceChecksFuture: Future[Seq[BalanceCheck]] =
        entityAccess
          .newQuery[BalanceCheck]()
          .filter(ModelFields.BalanceCheck.moneyReservoirCode === reservoir.code)
          .filter(
            input.year match {
              case None => DbQuery.Filter.NullFilter()
              case Some(year) =>
                filterInRange(
                  ModelFields.BalanceCheck.checkDate,
                  oldestBalanceDate,
                  DatedMonth.allMonthsIn(year).last.startTimeOfNextMonth,
                )
            }
          )
          .data()

      val transactionsFuture: Future[Seq[Transaction]] =
        entityAccess
          .newQuery[Transaction]()
          .filter(ModelFields.Transaction.moneyReservoirCode === reservoir.code)
          .filter(
            input.year match {
              case None => DbQuery.Filter.NullFilter()
              case Some(year) =>
                filterInRange(
                  ModelFields.Transaction.transactionDate,
                  oldestBalanceDate,
                  DatedMonth.allMonthsIn(year).last.startTimeOfNextMonth,
                )
            }
          )
          .data()
      val balanceChecks: Seq[BalanceCheck] = await(balanceChecksFuture)
      val transactions: Seq[Transaction] = await(transactionsFuture)

      val mergedRows: Seq[Entity] = (transactions ++ balanceChecks).sortBy {
        case trans: Transaction => (trans.transactionDate, trans.createdDate)
        case bc: BalanceCheck   => (bc.checkDate, bc.createdDate)
      }
      val dateToBalanceFunction: DateToBalanceFunction = {
        val builder = new DateToBalanceFunction.Builder(oldestBalanceDate, initialBalance)
        mergedRows.foreach {
          case transaction: Transaction =>
            builder.incrementLatestBalance(transaction.transactionDate, transaction.flow)
          case balanceCheck: BalanceCheck =>
            builder.addBalanceUpdate(balanceCheck.checkDate, balanceCheck.balance)
        }
        builder.result
      }

      val monthsInPeriod: Seq[DatedMonth] = input.year match {
        case Some(year) => DatedMonth.allMonthsIn(year)
        case None =>
          mergedRows match {
            case Seq() => Seq()
            case _ =>
              def entityToDate(entity: Entity): LocalDateTime = {
                entity match {
                  case trans: Transaction => trans.transactionDate
                  case bc: BalanceCheck   => bc.checkDate
                }
              }
              DatedMonth.monthsInClosedRange(
                DatedMonth.containing(entityToDate(mergedRows.head)),
                Seq(DatedMonth.current, DatedMonth.containing(entityToDate(mergedRows.last))).max,
              )
          }
      }

      InflationGains(
        monthToGains = monthsInPeriod.map { month =>
          val gain: ReferenceMoney = {
            def gainFromMoney(date: LocalDateTime, amount: MoneyWithGeneralCurrency): ReferenceMoney = {
              val valueAtDate = amount.withDate(date).exchangedForReferenceCurrency()
              val valueAtEnd = amount.withDate(month.startTimeOfNextMonth).exchangedForReferenceCurrency()
              val correctedValueAtDate =
                amount.withDate(date).exchangedForReferenceCurrency(correctForInflation = true)
              val correctedValueAtEnd = amount
                .withDate(month.startTimeOfNextMonth)
                .exchangedForReferenceCurrency(correctForInflation = true)

              // Subtract currency fluctuation effects, which is already handled in SummaryExchangeRateGainsStoreFactory
              (correctedValueAtEnd - correctedValueAtDate) - (valueAtEnd - valueAtDate)
            }

            val gainFromIntialMoney = gainFromMoney(month.startTime, dateToBalanceFunction(month.startTime))
            val gainFromUpdates =
              dateToBalanceFunction
                .updatesInRange(month)
                .map { case (date, DateToBalanceFunction.Update(balance, changeComparedToLast)) =>
                  gainFromMoney(date, changeComparedToLast)
                }
                .sum
            gainFromIntialMoney + gainFromUpdates
          }
          month -> GainsForMonth.forSingle(reservoir, gain)
        }.toMap,
        impactingTransactionIds = transactions.toStream.map(_.id).toSet,
        impactingBalanceCheckIds = (balanceChecks.toStream ++ oldestRelevantBalanceCheck).map(_.id).toSet,
      )
    }

    private def isRelevantReservoir(reservoir: MoneyReservoir): Boolean = {
      isRelevantAccount(reservoir.owner)
    }

    private def isRelevantAccount(account: Account): Boolean = {
      input.account match {
        case None    => true
        case Some(a) => account == a
      }
    }
    private def filterInRange[E](
        field: ModelField[LocalDateTime, E],
        start: LocalDateTime,
        end: LocalDateTime,
    ): DbQuery.Filter[E] = {
      (field >= start) && (field < end)
    }
  }

  /* override */
  protected case class Input(account: Option[Account], year: Option[Int])
}

object SummaryInflationGainsStoreFactory {

  private def combineMapValues[K, V](maps: Seq[Map[K, V]])(valueCombiner: Seq[V] => V): Map[K, V] = {
    val keys = maps.map(_.keySet).reduceOption(_ union _) getOrElse Set()
    keys.map(key => key -> valueCombiner(maps.filter(_ contains key).map(_.apply(key)))).toMap
  }

  case class InflationGains(
      monthToGains: Map[DatedMonth, GainsForMonth],
      protected override val impactingTransactionIds: Set[Long],
      protected override val impactingBalanceCheckIds: Set[Long],
  ) extends EntriesStore.StateTrait {
    def gainsForMonth(month: DatedMonth): GainsForMonth = monthToGains.getOrElse(month, GainsForMonth.empty)
    def nonEmpty: Boolean = this != InflationGains.empty
  }

  object InflationGains {
    val empty: InflationGains = InflationGains(Map(), Set(), Set())

    def sum(gains: Seq[InflationGains]): InflationGains = InflationGains(
      monthToGains = combineMapValues(gains.map(_.monthToGains))(GainsForMonth.sum),
      impactingTransactionIds = gains.map(_.impactingTransactionIds).reduceOption(_ union _) getOrElse Set(),
      impactingBalanceCheckIds = gains.map(_.impactingBalanceCheckIds).reduceOption(_ union _) getOrElse Set(),
    )
  }

  case class GainsForMonth private (reservoirToGains: Map[MoneyReservoir, ReferenceMoney]) {
    reservoirToGains.values.foreach(gain => require(!gain.isZero))

    lazy val total: ReferenceMoney = reservoirToGains.values.sum
    def nonEmpty: Boolean = reservoirToGains.nonEmpty
  }
  object GainsForMonth {
    val empty: GainsForMonth = GainsForMonth(Map())

    def forSingle(reservoir: MoneyReservoir, gain: ReferenceMoney): GainsForMonth = {
      if (gain.isZero) {
        empty
      } else {
        GainsForMonth(Map(reservoir -> gain))
      }
    }

    def sum(gains: Seq[GainsForMonth]): GainsForMonth =
      GainsForMonth(
        reservoirToGains = combineMapValues(gains.map(_.reservoirToGains))(_.sum).filterNot(_._2.isZero)
      )
  }
}
