package flux.react.app.transactionviews

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.CollectionUtils.asMap
import common.time.{Clock, DatedMonth}
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.RouterContext
import flux.react.ReactVdomUtils._
import flux.react.uielements
import flux.stores.entries.SummaryExchangeRateGainsStoreFactory.{GainsForMonth, GainsForYear}
import flux.stores.entries.SummaryForYearStoreFactory.{SummaryCell, SummaryForYear}
import flux.stores.entries._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.TransactionGroup
import models.accounting.config.Account.SummaryTotalRowDef
import models.accounting.config.{Account, Category, Config}
import models.accounting.money.{Currency, ExchangeRateManager, ReferenceMoney}
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
  def apply(account: Account,
            query: String,
            yearLowerBound: Int,
            expandedYear: Int,
            onShowHiddenYears: Callback,
            onSetExpandedYear: Int => Callback)(implicit router: RouterContext): VdomElement = {
    component(
      Props(
        account = account,
        query = query,
        yearLowerBound = yearLowerBound,
        expandedYear = expandedYear,
        onShowHiddenYears = onShowHiddenYears,
        onSetExpandedYear = onSetExpandedYear,
        router = router
      )).vdomElement
  }

  // **************** Private types ****************//

  private case class Props(account: Account,
                           query: String,
                           yearLowerBound: Int,
                           expandedYear: Int,
                           onShowHiddenYears: Callback,
                           onSetExpandedYear: Int => Callback,
                           router: RouterContext)

  private case class State(allYearsData: AllYearsData)

  private case class AllYearsData(allTransactionsYearRange: YearRange,
                                  private val yearsToData: ListMap[Int, AllYearsData.YearData]) {

    /**
      * Returns the categories of the transactions in the order configured for this account.
      *
      * If any transactions are not part of the configured seq, their categories are appended at the end. This should
      * normally not happen.
      */
    def categories(implicit props: Props): Seq[Category] = {
      props.account.categories.filter(categoriesSet) ++
        categoriesSet.filterNot(props.account.categories.contains)
    }

    def cell(category: Category, month: DatedMonth): SummaryCell =
      yearsToData(month.year).summary.cell(category, month)

    def totalWithoutCategories(categoriesToIgnore: Set[Category], month: DatedMonth): ReferenceMoney = {
      val summary = yearsToData(month.year).summary
      val exchangeRateData = yearsToData(month.year).exchangeRateGains

      summary.categories.filterNot(categoriesToIgnore).map(summary.cell(_, month).totalFlow).sum +
        exchangeRateData.gainsForMonth(month).total
    }
    def averageWithoutCategories(categoriesToIgnore: Set[Category], year: Int): ReferenceMoney = {
      monthsForAverage(year) match {
        case Seq() => ReferenceMoney(0)
        case months =>
          val total = months.map(totalWithoutCategories(categoriesToIgnore, _)).sum
          total / months.size
      }
    }

    def years: Seq[Int] = yearsToData.toVector.map(_._1)
    def yearlyAverage(year: Int, category: Category): ReferenceMoney = {
      val yearData = yearsToData(year)
      monthsForAverage(year) match {
        case Seq() => ReferenceMoney(0)
        case months =>
          val totalFlow = months.map(yearData.summary.cell(category, _).totalFlow).sum
          totalFlow / months.size
      }
    }

    def monthsForAverage(year: Int): Seq[DatedMonth] = {
      val pastMonths = DatedMonth.allMonthsIn(year).filter(_ < DatedMonth.containing(clock.now))
      if (allTransactionsYearRange.isEmpty) {
        Seq()
      } else if (allTransactionsYearRange.firstYear == year) {
        pastMonths.filter(_ >= yearsToData(year).summary.months.min)
      } else {
        pastMonths
      }
    }

    lazy val hasExchangeRateGains: Boolean = yearsToData.values.exists(_.exchangeRateGains.nonEmpty)
    def exchangeRateGains(month: DatedMonth): GainsForMonth =
      yearsToData(month.year).exchangeRateGains.gainsForMonth(month)
    def averageExchangeRateGains(year: Int): ReferenceMoney =
      DatedMonth.allMonthsIn(year).map(exchangeRateGains(_).total).sum

    private lazy val categoriesSet: Set[Category] = {
      for {
        yearData <- yearsToData.values
        category <- yearData.summary.categories
      } yield category
    }.toSet
  }
  private object AllYearsData {
    val empty: AllYearsData =
      AllYearsData(allTransactionsYearRange = YearRange.empty, yearsToData = ListMap())

    def builder(allTransactionsYearRange: YearRange): Builder = new Builder(allTransactionsYearRange)

    case class YearData(summary: SummaryForYear, exchangeRateGains: GainsForYear)

    final class Builder(allTransactionsYearRange: YearRange) {
      val yearsToData: mutable.LinkedHashMap[Int, AllYearsData.YearData] = mutable.LinkedHashMap()

      def addYear(year: Int, summary: SummaryForYear, exchangeRateGains: GainsForYear): Builder = {
        yearsToData.put(year, YearData(summary, exchangeRateGains))
        this
      }

      def result: AllYearsData = AllYearsData(allTransactionsYearRange, ListMap(yearsToData.toSeq: _*))
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
      implicit val data = state.allYearsData
      implicit val router = props.router

      <.table(
        ^.className := "table table-bordered table-hover table-condensed table-summary",
        <.thead(
          // **************** Year header **************** //
          <.tr(
            columns.flatMap {
              case TitleColumn =>
                Some(<.th(^.key := "title", Currency.default.symbol))
              case OmittedYearsColumn(yearRange) =>
                Some(
                  <.th(
                    ^.key := "omitted-years",
                    <.a(^.onClick --> props.onShowHiddenYears, yearRange.firstYear, <<.ifThen(yearRange.size > 1)("-"))))
              case MonthColumn(month) =>
                None
              case AverageColumn(year) =>
                Some(
                  <.th(
                    ^.key := year,
                    ^.colSpan := columnsForYear(year, expandedYear = props.expandedYear).size,
                    <.a(^.onClick --> props.onSetExpandedYear(year), year)))
            }.toVdomArray
          ),
          // **************** Month header **************** //
          <.tr(
            columns.map {
              case TitleColumn =>
                <.th(^.key := "title", i18n("facto.category"))
              case OmittedYearsColumn(yearRange) =>
                <.th(
                  ^.key := "omitted-years",
                  <.a(^.onClick --> props.onShowHiddenYears, <<.ifThen(yearRange.size > 1)(yearRange.lastYear)))
              case MonthColumn(month) =>
                <.th(^.key := s"${month.year}-${month.month}", month.abbreviation)
              case AverageColumn(year) =>
                <.th(^.key := s"avg-$year", i18n("facto.avg"))
            }.toVdomArray
          )
        ),
        <.tbody(
          // **************** Categories data **************** //
          {
            for (category <- data.categories) yield {
              <.tr(
                ^.key := category.code,
                columns.map {
                  case TitleColumn =>
                    <.td(^.key := "title", category.name)
                  case OmittedYearsColumn(_) =>
                    <.td(^.key := "omitted-years", "...")
                  case MonthColumn(month) =>
                    val cellData = data.cell(category, month)
                    <.td(
                      ^.key := s"avg-${category.code}-${month.year}-${month.month}",
                      ^^.classes(cellClasses(month)),
                      uielements.UpperRightCorner(
                        cornerContent = <<.ifThen(cellData.nonEmpty)(s"(${cellData.transactions.size})"))(
                        centralContent = if (cellData.nonEmpty) cellData.totalFlow.formatFloat else ""
                      )
                    )
                  case AverageColumn(year) =>
                    <.td(
                      ^.key := s"avg-${category.code}-$year",
                      ^.className := "average",
                      data.yearlyAverage(year, category).formatFloat)
                }.toVdomArray
              )
            }
          }.toVdomArray,
          // **************** Exchange rate gains data **************** //
          ^^.ifThen(data.hasExchangeRateGains) {
            <.tr(
              ^.key := "exchange-rate-gains",
              columns.map {
                case TitleColumn =>
                  <.td(^.key := "title", i18n("facto.exchange-rate-gains"))
                case OmittedYearsColumn(_) =>
                  <.td(^.key := "omitted-years", "...")
                case MonthColumn(month) =>
                  val cellData = data.exchangeRateGains(month)
                  <.td(
                    ^.key := s"gain-${month.year}-${month.month}",
                    ^^.classes(cellClasses(month)),
                    uielements.UpperRightCorner(
                      cornerContent = <<.ifThen(cellData.nonEmpty)(s"(${cellData.reservoirToGains.size})"))(
                      centralContent = if (cellData.nonEmpty) cellData.total.formatFloat else ""
                    )
                  )
                case AverageColumn(year) =>
                  <.td(
                    ^.key := s"avg-$year",
                    ^.className := "average",
                    data.averageExchangeRateGains(year).formatFloat)
              }.toVdomArray
            )
          },
          // **************** Total rows **************** //
          props.account.summaryTotalRows.zipWithIndex.map {
            case (SummaryTotalRowDef(rowTitleHtml, categoriesToIgnore), rowIndex) =>
              <.tr(
                ^.key := s"total-$rowIndex",
                ^.className := s"total total-$rowIndex",
                columns.map {
                  case TitleColumn =>
                    <.td(^.key := "title", ^.className := "title", ^.dangerouslySetInnerHtml := rowTitleHtml)
                  case OmittedYearsColumn(_) =>
                    <.td(^.key := "omitted-years", "...")
                  case MonthColumn(month) =>
                    val total = data.totalWithoutCategories(categoriesToIgnore, month)
                    <.td(
                      ^.key := s"total-$rowIndex-${month.year}-${month.month}",
                      ^^.classes(cellClasses(month)),
                      <<.ifThen(total.nonZero) { total.formatFloat }
                    )
                  case AverageColumn(year) =>
                    <.td(
                      ^.key := s"average-$rowIndex-$year",
                      ^.className := "average",
                      data.averageWithoutCategories(categoriesToIgnore, year).formatFloat)
                }.toVdomArray
              )
          }.toVdomArray
        )
      )
    }

    private def doStateUpdate(props: Props): Unit = {
      val (data, usedStores): (AllYearsData, Set[EntriesStore[_]]) = {
        val yearsStore = summaryYearsStoreFactory.get(props.account)
        val allTransactionsYearRange = yearsStore.state
        val yearRange = allTransactionsYearRange
          .copyIncluding(clock.now.getYear)
          .copyWithLowerBound(props.yearLowerBound)
          .copyIncluding(props.expandedYear)

        val dataBuilder = AllYearsData.builder(allTransactionsYearRange)
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

    private def ifThenSeq[V](condition: Boolean, value: V): Seq[V] = if (condition) Seq(value) else Seq()

    private def cellClasses(month: DatedMonth)(implicit data: AllYearsData): Seq[String] =
      Seq("cell") ++
        ifThenSeq(month.contains(clock.now), "current-month") ++
        ifThenSeq(data.monthsForAverage(month.year).contains(month), "month-for-averages")

    private sealed trait Column
    private case object TitleColumn extends Column
    private case class OmittedYearsColumn(yearRange: YearRange) extends Column
    private case class MonthColumn(month: DatedMonth) extends Column
    private case class AverageColumn(year: Int) extends Column
    private def columnsForYear(year: Int, expandedYear: Int): Seq[Column] = {
      if (year == expandedYear) {
        DatedMonth.allMonthsIn(year).map(MonthColumn) :+ AverageColumn(year)
      } else {
        Seq(AverageColumn(year))
      }
    }
    private def columns(implicit props: Props, data: AllYearsData): Seq[Column] = {
      val omittedYears = data.allTransactionsYearRange.copyLessThan(data.years.min)

      Seq(TitleColumn) ++
        ifThenSeq(omittedYears.nonEmpty, OmittedYearsColumn(omittedYears)) ++
        data.years.flatMap(year => columnsForYear(year = year, expandedYear = props.expandedYear))
    }
  }
}
