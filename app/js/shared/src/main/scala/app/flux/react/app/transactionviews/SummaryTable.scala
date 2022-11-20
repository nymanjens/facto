package app.flux.react.app.transactionviews

import app.common.accounting.TemplateMatcher
import hydro.common.CollectionUtils.ifThenSeq
import hydro.common.I18n
import hydro.common.Annotations.visibleForTesting
import hydro.common.Tags
import app.common.money.Currency
import app.common.money.ExchangeRateManager
import app.common.money.ReferenceMoney
import app.common.time.DatedMonth
import app.common.time.YearRange
import app.flux.react.uielements
import app.flux.react.uielements.MoneyWithCurrency
import app.flux.router.AppPages
import app.flux.stores.entries._
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory.ExchangeRateGains
import app.flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryCell
import app.flux.stores.entries.factories.SummaryForYearStoreFactory.SummaryForYear
import app.flux.stores.entries.factories.CashFlowEntriesStoreFactory
import app.flux.stores.entries.factories.SummaryExchangeRateGainsStoreFactory
import app.flux.stores.entries.factories.SummaryForYearStoreFactory
import app.flux.stores.entries.factories.SummaryYearsStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Account.SummaryTotalRowDef
import app.models.accounting.config.Account
import app.models.accounting.config.Category
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.ReactVdomUtils._
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.BootstrapTags
import hydro.flux.router.RouterContext
import hydro.flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.ListMap
import scala.collection.immutable.Seq
import scala.collection.mutable

private[transactionviews] final class SummaryTable(implicit
    summaryYearsStoreFactory: SummaryYearsStoreFactory,
    summaryForYearStoreFactory: SummaryForYearStoreFactory,
    summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
    cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
    entityAccess: AppJsEntityAccess,
    user: User,
    clock: Clock,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    templateMatcher: TemplateMatcher,
    moneyWithCurrency: MoneyWithCurrency,
) {

  private val component = {
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState(State(allYearsData = AllYearsData.empty, dataIsLoading = true))
      .renderBackend[Backend]
      .componentWillMount(scope => scope.backend.willMount(scope.props))
      .componentWillUnmount(scope => scope.backend.willUnmount())
      .componentWillReceiveProps(scope => scope.backend.willReceiveProps(scope.nextProps))
      .build
  }

  // **************** API ****************//
  def apply(
      key: String,
      account: Account,
      query: String,
      yearLowerBound: Int,
      expandedYear: Int,
      showYearlyTotal: Boolean,
      onShowHiddenYears: Callback,
      onSetExpandedYear: Int => Callback,
      onShowYearlyTotalToggle: Callback,
  )(implicit router: RouterContext): VdomElement = {
    component
      .withKey(key)
      .apply(
        Props(
          account = account,
          query = query,
          yearLowerBound = yearLowerBound,
          expandedYear = expandedYear,
          showYearlyTotal = showYearlyTotal,
          onShowHiddenYears = onShowHiddenYears,
          onSetExpandedYear = onSetExpandedYear,
          onShowYearlyTotalToggle = onShowYearlyTotalToggle,
          router = router,
        )
      )
      .vdomElement
  }

  // **************** Private types ****************//

  private case class Props(
      account: Account,
      query: String,
      yearLowerBound: Int,
      expandedYear: Int,
      showYearlyTotal: Boolean, // instead of average
      onShowHiddenYears: Callback,
      onSetExpandedYear: Int => Callback,
      onShowYearlyTotalToggle: Callback,
      router: RouterContext,
  )

  private case class State(allYearsData: AllYearsData, dataIsLoading: Boolean)

  @visibleForTesting private[transactionviews] case class AllYearsData(
      allTransactionsYearRange: YearRange,
      private val yearsToData: ListMap[Int, AllYearsData.YearData],
      netWorth: ReferenceMoney,
  ) {

    /**
     * Returns the categories of the transactions in the order configured for this account.
     *
     * If any transactions are not part of the configured seq, their categories are appended at the end. This should
     * normally not happen.
     */
    def categories(implicit account: Account): Seq[Category] = {
      account.categories.filter(categoriesSet) ++ categoriesSet.filterNot(account.categories.contains)
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
    def totalWithoutCategories(categoriesToIgnore: Set[Category], year: Int): ReferenceMoney = {
      DatedMonth.allMonthsIn(year).map(totalWithoutCategories(categoriesToIgnore, _)).sum
    }

    def years: Seq[Int] = yearsToData.toVector.map(_._1)
    def yearlyAverage(year: Int, category: Category): ReferenceMoney = {
      monthsForAverage(year) match {
        case Seq() => ReferenceMoney(0)
        case months =>
          val totalFlow = months.map(yearsToData(year).summary.cell(category, _).totalFlow).sum
          totalFlow / months.size
      }
    }
    def yearlyTotal(year: Int, category: Category): ReferenceMoney = {
      DatedMonth.allMonthsIn(year).map(month => yearsToData(year).summary.cell(category, month).totalFlow).sum
    }

    def monthsForAverage(year: Int): Seq[DatedMonth] = {
      val pastMonths = DatedMonth.allMonthsIn(year).filter(_ < DatedMonth.containing(clock.now))
      if (allTransactionsYearRange.isEmpty || yearsToData(year).summary.months.isEmpty) {
        Seq()
      } else if (allTransactionsYearRange.firstYear == year) {
        pastMonths.filter(_ >= yearsToData(year).summary.months.min)
      } else {
        pastMonths
      }
    }

    lazy val currenciesWithExchangeRateGains: Seq[Currency] =
      yearsToData.values.toStream.flatMap(_.exchangeRateGains.currencies).distinct.toVector
    def exchangeRateGains(currency: Currency, month: DatedMonth): ReferenceMoney =
      yearsToData(month.year).exchangeRateGains.gainsForMonth(month).gains(currency)
    def averageExchangeRateGains(currency: Currency, year: Int): ReferenceMoney = {
      monthsForAverage(year) match {
        case Seq()  => ReferenceMoney(0)
        case months => months.map(exchangeRateGains(currency, _)).sum / months.size
      }
    }
    def totalExchangeRateGains(currency: Currency, year: Int): ReferenceMoney = {
      DatedMonth.allMonthsIn(year).map(exchangeRateGains(currency, _)).sum
    }

    private lazy val categoriesSet: Set[Category] = {
      for {
        yearData <- yearsToData.values
        category <- yearData.summary.categories
      } yield category
    }.toSet
  }
  @visibleForTesting private[transactionviews] object AllYearsData {
    val empty: AllYearsData =
      AllYearsData(
        allTransactionsYearRange = YearRange.empty,
        yearsToData = ListMap(),
        netWorth = ReferenceMoney(0),
      )

    def builder(allTransactionsYearRange: YearRange): Builder = new Builder(allTransactionsYearRange)

    case class YearData(summary: SummaryForYear, exchangeRateGains: ExchangeRateGains)

    final class Builder(allTransactionsYearRange: YearRange) {
      var netWorth: ReferenceMoney = ReferenceMoney(0)
      val yearsToData: mutable.LinkedHashMap[Int, AllYearsData.YearData] = mutable.LinkedHashMap()

      def addYear(year: Int, summary: SummaryForYear, exchangeRateGains: ExchangeRateGains): Builder = {
        yearsToData.put(year, YearData(summary, exchangeRateGains))
        this
      }

      def addToNetWorth(money: ReferenceMoney): Builder = {
        netWorth = netWorth + money
        this
      }

      def result: AllYearsData =
        AllYearsData(allTransactionsYearRange, ListMap(yearsToData.toSeq: _*), netWorth)
    }
  }

  private class Backend($ : BackendScope[Props, State]) extends StateStore.Listener {

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
      implicit val account = props.account
      implicit val router = props.router

      <.table(
        ^.className := "table table-bordered table-hover table-condensed table-summary",
        <.thead(
          // **************** Title header **************** //
          <.tr(
            ^.className := "info",
            <.th(
              ^.colSpan := columns.size,
              <.span(^.className := "primary-title", account.longName),
              <.span(^.className := "secondary-title", i18n("app.net-worth"), " ", data.netWorth.toString),
            ),
          ),
          // **************** Year header **************** //
          <.tr(
            columns.flatMap {
              case TitleColumn =>
                Some(<.th(^.key := "title", Currency.default.symbol))
              case OmittedYearsColumn(yearRange) =>
                Some(
                  <.th(
                    ^.key := "omitted-years",
                    <.a(
                      ^.href := "javascript:void(0)",
                      ^.onClick --> props.onShowHiddenYears,
                      yearRange.firstYear,
                      <<.ifThen(yearRange.size > 1)("-"),
                    ),
                  )
                )
              case MonthColumn(month) =>
                None
              case AverageColumn(year) =>
                Some(
                  <.th(
                    ^.key := year,
                    ^.colSpan := columnsForYear(year, expandedYear = props.expandedYear).size,
                    <.a(^.href := "javascript:void(0)", ^.onClick --> props.onSetExpandedYear(year), year),
                  )
                )
              case TotalColumn(year) =>
                Some(
                  <.th(
                    ^.key := year,
                    ^.colSpan := columnsForYear(year, expandedYear = props.expandedYear).size,
                    <.a(^.href := "javascript:void(0)", ^.onClick --> props.onSetExpandedYear(year), year),
                  )
                )
            }.toVdomArray
          ),
          // **************** Month header **************** //
          <.tr(
            columns.map {
              case TitleColumn =>
                <.th(^.key := "title", i18n("app.category"))
              case OmittedYearsColumn(yearRange) =>
                <.th(
                  ^.key := "omitted-years",
                  <.a(
                    ^.href := "javascript:void(0)",
                    ^.onClick --> props.onShowHiddenYears,
                    <<.ifThen(yearRange.size > 1)(yearRange.lastYear),
                  ),
                )
              case MonthColumn(month) =>
                <.th(^.key := s"${month.year}-${month.month}", month.abbreviation)
              case AverageColumn(year) =>
                <.th(
                  ^.key := s"avg-$year",
                  <.a(
                    ^.href := "javascript:void(0)",
                    ^.onClick --> props.onShowYearlyTotalToggle,
                    i18n("app.avg"),
                  ),
                )
              case TotalColumn(year) =>
                <.th(
                  ^.key := s"total-$year",
                  <.a(
                    ^.href := "javascript:void(0)",
                    ^.onClick --> props.onShowYearlyTotalToggle,
                    i18n("app.total_column"),
                  ),
                )
            }.toVdomArray
          ),
        ),
        <.tbody(
          // **************** Categories data **************** //
          if ((data.categories.isEmpty && props.query.isEmpty) || state.dataIsLoading) {
            // No data or fallback in case data is still loading
            {
              for (category <- account.categories) yield {
                <.tr(
                  ^.key := category.code,
                  columns.map {
                    case TitleColumn =>
                      <.td(^.key := "title", category.name)
                    case OmittedYearsColumn(_) =>
                      <.td(^.key := "empty-omitted-years", "...")
                    case MonthColumn(month) =>
                      <.td(^.key := s"empty-avg-${month.year}-${month.month}", ^.className := "cell")
                    case AverageColumn(year) =>
                      <.td(^.key := s"empty-avg-$year", ^.className := "average")
                    case TotalColumn(year) =>
                      <.td(^.key := s"empty-total-$year", ^.className := "average")
                  }.toVdomArray,
                )
              }
            }.toVdomArray
          } else {
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
                          cornerContent = <<.ifThen(cellData.nonEmpty)(s"(${cellData.transactions.size})")
                        )(
                          /* centralContent = */
                          if (cellData.nonEmpty) cellData.totalFlow.formatFloat else "",
                          ^^.ifThen(cellData.nonEmpty) {
                            <.div(
                              ^.className := "entries",
                              (for (transaction <- cellData.transactions) yield {
                                val maybeTemplateIcon: Option[VdomNode] =
                                  templateMatcher.getMatchingTemplate(Seq(transaction)) map { template =>
                                    <.i(^.key := "template-icon", ^.className := template.iconClass)
                                  }
                                val tagIndications = transaction.tags
                                  .map(tag =>
                                    Bootstrap.Label(BootstrapTags.toStableVariant(tag))(
                                      ^.key := tag,
                                      tag,
                                    ): VdomNode
                                  )
                                <.div(
                                  ^.key := transaction.id,
                                  router.anchorWithHrefTo(
                                    AppPages.EditTransactionGroup(transaction.transactionGroupId)
                                  )(
                                    moneyWithCurrency(transaction.flow),
                                    " - ",
                                    <<.joinWithSpaces(
                                      maybeTemplateIcon.toVector ++
                                        tagIndications :+
                                        (transaction.description: VdomNode)
                                    ),
                                  ),
                                )
                              }).toVdomArray,
                            )
                          },
                        ),
                      )
                    case AverageColumn(year) =>
                      <.td(
                        ^.key := s"avg-${category.code}-$year",
                        ^.className := "average",
                        data.yearlyAverage(year, category).formatFloat,
                      )
                    case TotalColumn(year) =>
                      <.td(
                        ^.key := s"total-${category.code}-$year",
                        ^.className := "average",
                        data.yearlyTotal(year, category).formatFloat,
                      )
                  }.toVdomArray,
                )
              }
            }.toVdomArray
          },
          // **************** Exchange rate gains data **************** //
          ^^.ifThen(props.query.isEmpty) {
            (for (currency <- data.currenciesWithExchangeRateGains) yield {
              <.tr(
                ^.key := s"exchange-rate-gains-${currency.code}",
                columns.map {
                  case TitleColumn =>
                    <.td(^.key := "title", i18n("app.exchange-rate-gains"), " ", currency.code)
                  case OmittedYearsColumn(_) =>
                    <.td(^.key := "omitted-years", "...")
                  case MonthColumn(month) =>
                    <.td(
                      ^.key := s"gain-${currency.code}-${month.year}-${month.month}",
                      ^^.classes(cellClasses(month)),
                      data.exchangeRateGains(currency, month).formatFloat,
                    )
                  case AverageColumn(year) =>
                    <.td(
                      ^.key := s"avg-$year",
                      ^.className := "average",
                      data.averageExchangeRateGains(currency, year).formatFloat,
                    )
                  case TotalColumn(year) =>
                    <.td(
                      ^.key := s"total-$year",
                      ^.className := "average",
                      data.totalExchangeRateGains(currency, year).formatFloat,
                    )
                }.toVdomArray,
              )
            }).toVdomArray
          },
          // **************** Total rows **************** //
          ^^.ifThen(props.query.isEmpty || data.categories.nonEmpty) {
            {
              props.query match {
                case "" => props.account.summaryTotalRows
                case _ if data.categories.nonEmpty =>
                  Seq(
                    SummaryTotalRowDef(
                      rowTitleHtml = s"<b>${i18n("app.total")}</b>",
                      categoriesToIgnore = Set(),
                    )
                  )
                case _ => Seq()
              }
            }.zipWithIndex.map { case (SummaryTotalRowDef(rowTitleHtml, categoriesToIgnore), rowIndex) =>
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
                      <<.ifThen(total.nonZero) {
                        total.formatFloat
                      },
                    )
                  case AverageColumn(year) =>
                    <.td(
                      ^.key := s"average-$rowIndex-$year",
                      ^.className := "average",
                      data.averageWithoutCategories(categoriesToIgnore, year).formatFloat,
                    )
                  case TotalColumn(year) =>
                    <.td(
                      ^.key := s"total-$rowIndex-$year",
                      ^.className := "average",
                      data.totalWithoutCategories(categoriesToIgnore, year).formatFloat,
                    )
                }.toVdomArray,
              )
            }.toVdomArray
          },
        ),
      )
    }

    private def doStateUpdate(props: Props): Unit = {
      val (data, usedStores): (AllYearsData, Set[EntriesStore[_]]) = {
        val yearsStore = summaryYearsStoreFactory.get(props.account)
        val allTransactionsYearRange = yearsStore.state.map(_.yearRange) getOrElse
          YearRange.closed(clock.now.getYear - 1, clock.now.getYear)
        val yearRange = allTransactionsYearRange
          .copyIncluding(clock.now.getYear)
          .copyWithLowerBound(props.yearLowerBound)
          .copyIncluding(props.expandedYear)

        val dataBuilder = AllYearsData.builder(allTransactionsYearRange)
        val usedStores: mutable.Set[EntriesStore[_]] = mutable.Set(yearsStore)
        for (year <- yearRange.toSeq) {
          val summaryForYearStore =
            summaryForYearStoreFactory.get(account = props.account, year = year, query = props.query)
          val exchangeRateGainsStore = props.query match {
            case "" => Some(summaryExchangeRateGainsStoreFactory.get(account = props.account, year = year))
            case _  => None
          }

          dataBuilder.addYear(
            year,
            summaryForYearStore.state getOrElse SummaryForYear.empty,
            exchangeRateGainsStore.flatMap(_.state) getOrElse ExchangeRateGains.empty,
          )
          usedStores += summaryForYearStore
          usedStores ++= exchangeRateGainsStore.toSeq
        }
        for (reservoir <- accountingConfig.visibleReservoirs) {
          if (reservoir.owner == props.account) {
            val store = cashFlowEntriesStoreFactory.get(
              moneyReservoir = reservoir,
              maxNumEntries = CashFlow.minNumEntriesPerReservoir,
            )

            if (store.state.isDefined) {
              dataBuilder.addToNetWorth(
                store.state.get.entries.lastOption
                  .map(_.entry.balance.withDate(clock.now).exchangedForReferenceCurrency()) getOrElse
                  ReferenceMoney(0)
              )
            }

            usedStores += store
          }
        }
        (dataBuilder.result, usedStores.toSet)
      }

      $.modState(_.copy(allYearsData = data, dataIsLoading = usedStores.exists(!_.state.isDefined))).runNow()
      usedStores.filterNot(allRegisteredStores).foreach(_.register(this))
      allRegisteredStores.filterNot(usedStores).foreach(_.deregister(this))
      allRegisteredStores = usedStores
    }

    private def cellClasses(month: DatedMonth)(implicit data: AllYearsData): Seq[String] =
      Seq("cell") ++
        ifThenSeq(month.contains(clock.now), "current-month") ++
        ifThenSeq(data.monthsForAverage(month.year).contains(month), "month-for-averages")

    private sealed trait Column
    private case object TitleColumn extends Column
    private case class OmittedYearsColumn(yearRange: YearRange) extends Column
    private case class MonthColumn(month: DatedMonth) extends Column
    private case class AverageColumn(year: Int) extends Column
    private case class TotalColumn(year: Int) extends Column
    private def columnsForYear(year: Int, expandedYear: Int)(implicit props: Props): Seq[Column] = {
      val avgOrTotal = if (props.showYearlyTotal) TotalColumn(year) else AverageColumn(year)
      if (year == expandedYear) {
        DatedMonth.allMonthsIn(year).map(MonthColumn) :+ avgOrTotal
      } else {
        Seq(avgOrTotal)
      }
    }
    private def columns(implicit props: Props, data: AllYearsData): Seq[Column] = {
      val omittedYears = data.allTransactionsYearRange.copyLessThan(props.yearLowerBound)

      Seq(TitleColumn) ++
        ifThenSeq(omittedYears.nonEmpty, OmittedYearsColumn(omittedYears)) ++
        data.years.flatMap(year => columnsForYear(year = year, expandedYear = props.expandedYear))
    }
  }
}
