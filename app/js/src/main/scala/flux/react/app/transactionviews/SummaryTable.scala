package flux.react.app.transactionviews

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.CollectionUtils.asMap
import common.time.{Clock, DatedMonth}
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.RouterContext
import flux.react.ReactVdomUtils._
import flux.react.uielements
import flux.stores.entries.SummaryExchangeRateGainsStoreFactory.GainsForYear
import flux.stores.entries.SummaryForYearStoreFactory.SummaryForYear
import flux.stores.entries._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.TransactionGroup
import models.accounting.config.{Account, Category, Config}
import models.accounting.money.{Currency, ExchangeRateManager}
import models.{EntityAccess, User}

import scala.collection.immutable.{ListMap, Seq}
import scala.collection.mutable

private[transactionviews] final class SummaryTable(
    implicit summaryYearsStoreFactory: SummaryYearsStoreFactory,
    summaryForYearStoreFactory: SummaryForYearStoreFactory,
    summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
    entityAccess: EntityAccess,
    user: User,
    clock: Clock,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n) {

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState(State(allYearsData = AllYearsData.empty))
      .renderBackend[Backend]
      .componentWillMount(scope => scope.backend.willMount(scope.props))
      .componentWillUnmount(scope => scope.backend.willUnmount())
      .componentWillReceiveProps(scope => scope.backend.willReceiveProps(scope.nextProps))
      .build
  }

  // **************** API ****************//
  def apply(account: Account, query: String, hideColumnsOlderThanYear: Int, expandedYear: Int)(
      implicit router: RouterContext): VdomElement = {
    component(
      Props(
        account = account,
        query = query,
        hideColumnsOlderThanYear = hideColumnsOlderThanYear,
        expandedYear = expandedYear,
        router = router)).vdomElement
  }

  // **************** Private types ****************//

  private case class Props(account: Account,
                           query: String,
                           hideColumnsOlderThanYear: Int,
                           expandedYear: Int,
                           router: RouterContext)

  private case class State(allYearsData: AllYearsData)

  private case class AllYearsData(yearsToData: ListMap[Long, AllYearsData.YearData]) {
    def categories: Seq[Category] = ???
  }
  private object AllYearsData {
    val empty: AllYearsData = AllYearsData(ListMap())

    def builder(): Builder = new Builder

    final class YearData(summaryForYear: SummaryForYear, gainsForYear: GainsForYear) {
//      def monthsToData: Map[DatedMonth, Month
      def months(implicit props: Props): Seq[DatedMonth] = ???
    }

    final class Builder {
      val yearsToData: mutable.ListMap[Long, AllYearsData.YearData] = mutable.ListMap()

      def addYear(year: Long, summaryForYear: SummaryForYear, gainsForYear: GainsForYear): Builder = {
        yearsToData.put(year, new YearData(summaryForYear, gainsForYear))
        this
      }

      def result: AllYearsData = AllYearsData(ListMap(yearsToData.toVector: _*))
    }
  }

  private class Backend($ : BackendScope[Props, State]) extends EntriesStore.Listener {

    private var allRegisteredStores: Set[EntriesStore[_]] = Set()

    def willMount(props: Props): Callback = LogExceptionsCallback {
      doStateUpdate(props)
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      allRegisteredStores.foreach(_.deregister(this))
      allRegisteredStores = Set()
    }

    def willReceiveProps(nextProps: Props): Callback = LogExceptionsCallback {
      doStateUpdate(nextProps)
    }

    override def onStateUpdate() = {
      doStateUpdate($.props.runNow())
    }

    def render(implicit props: Props, state: State) = logExceptions {
      val summary = state.allYearsData // TODO: Rename val to `data`
      implicit val router = props.router

      <.table(
        ^.className := "table table-bordered table-hover table-condensed table-summary",
        <.thead(
          <.tr(
            <.th(Currency.default.symbol), {
              for ((year, summaryForYear) <- summary.yearsToData) yield {
                <.th(
                  ^.key := year,
                  ^.colSpan := {
                    if (year == props.expandedYear) summaryForYear.months.size + 1 else 1
                  },
                  <.a(^.href := "#TODO", year)
                )
              }
            }.toVdomArray
          )
//          ,
//          <.tr(
//            <.th(i18n("facto.category")), {
//              for ((year, summaryForYear) <- summary.yearsToData) yield {
//                ifThenSeq(year == expandedYear, {
//                  for (month <- summaryForYear.months) yield {
//                    <.th(^.key := s"$year-${month.abbreviation}", month.abbreviation)
//                  }
//                }) :+ <.th(i18n("facto.avg"), ^.key := s"avg-$year")
//              }
//            }
//          )
        )
//        ,
//        <.tbody(
//          for (category <- summary.categories) yield {
//            <.tr(
//              ^.key := category.code,
//              <.td(category.name),
//              for ((year, summaryForYear) <- summary.yearsToData) yield {
//                if (year == expandedYear) {
//                  for (month <- summaryForYear.months) yield {
//                    <.td(
//                      ^^.classes(
//                        Seq("cell") ++ ifThenSeq(month.contains(clock.now), "current-month") ++ ifThenSeq(
//                          summary.monthRangeForAverages.contains(month),
//                          "month-for-averages")),
//                      uielements.UpperRightCorner(
//                        ^^.ifThen(summaryForYear.cell(category, month).entries.isEmpty)(
//                          s"(${entry.transactions.size})")) {
//                        {
//                          summaryForYear.cell(category, month).totalFlow match {
//                            case flow if summaryForYear.cell(category, month).entries.isEmpty => ""
//                            case flow => flow.formatFloat
//                          }
//                        }
//                      }
//                    )
//                  }
//                }
//              } :+ <.td(^.className := "average", summaryForYear.categoryToAverages(category).formatFloat)
//            )
//          },
//          for ((totalRowTitle, rowIndex) <- summary.totalRowTitles.zipWithIndex) yield {
//            <.tr(
//              ^.className := "total total-$rowIndex",
//              <.td(^.className := "title", totalRowTitle),
//              for ((year, summaryForYear) <- summary.yearsToData) yield {
//                if (year == expandedYear) {
//                  for ((month, totalThisMonth) <- summaryForYear.totalRows(rowIndex).monthToTotal) yield {
//                    <.td(
//                      ^^.classes(
//                        Seq("cell") ++ ifThenSeq(month.contains(clock.now), "current-month") ++ ifThenSeq(
//                          summary.monthRangeForAverages.contains(month),
//                          "month-for-averages")),
//                      ^^.ifThen(totalThisMonth.cents != 0) { totalThisMonth.formatFloat }
//                    )
//                  }
//                }
//                <.td(^.className := "average", summaryForYear.totalRows(rowIndex).yearlyAverage.formatFloat)
//              }
//            )
//          }
//        )
      )
    }

    private def ifThenSeq[V](condition: Boolean, value: V): Seq[V] = if (condition) Seq(value) else Seq()

    private def doStateUpdate(props: Props): Unit = {
      val (data, usedStores): (AllYearsData, Set[EntriesStore[_]]) = {
        val yearsStore = summaryYearsStoreFactory.get(props.account)
        val yearRange = yearsStore.state
          .copyIncluding(clock.now.getYear)
          .copyWithLowerBound(props.hideColumnsOlderThanYear)
          .copyIncluding(props.expandedYear)

        val dataBuilder = AllYearsData.builder()
        val usedStores: mutable.Set[EntriesStore[_]] = mutable.Set(yearsStore)
        for (year <- yearRange.toSeq) {
          val summaryForYearStore =
            summaryForYearStoreFactory.get(account = props.account, year = year, query = props.query)
          val exchangeRateGainsStore =
            summaryExchangeRateGainsStoreFactory.get(account = props.account, year = year)

          dataBuilder.addYear(year, summaryForYearStore.state, exchangeRateGainsStore.state)
          usedStores ++= Seq(summaryForYearStore, exchangeRateGainsStore)
        }
        (dataBuilder.result, usedStores.toSet)
      }

      $.modState(_.copy(allYearsData = data)).runNow()
      usedStores.filterNot(allRegisteredStores).foreach(_.register(this))
      allRegisteredStores.filterNot(usedStores).foreach(_.deregister(this))
      allRegisteredStores = usedStores
    }
  }
}
