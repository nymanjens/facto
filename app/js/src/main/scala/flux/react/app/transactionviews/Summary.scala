package flux.react.app.transactionviews

import common.Formatting._
import common.I18n
import common.time.Clock
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.entries.{
  AllEntriesStoreFactory,
  GeneralEntry,
  SummaryForYearStoreFactory,
  SummaryYearsStoreFactory
}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.{EntityAccess, User}
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq

final class Summary(implicit summaryTable: SummaryTable,
                    entityAccess: EntityAccess,
                    user: User,
                    clock: Clock,
                    accountingConfig: Config,
                    exchangeRateManager: ExchangeRateManager,
                    i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(
      State(
        includeUnrelatedAccounts = false,
        query = "",
        yearLowerBound = clock.now.getYear - 1,
        expandedYear = clock.now.getYear))
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          {
            for {
              account <- accountingConfig.personallySortedAccounts
              if state.includeUnrelatedAccounts || account.isMineOrCommon
            } yield {
              uielements.Panel(account.longName, key = account.code) {
                summaryTable(
                  account = account,
                  query = state.query,
                  yearLowerBound = state.yearLowerBound,
                  expandedYear = state.expandedYear,
                  onShowHiddenYears = $.modState(_.copy(yearLowerBound = Int.MinValue)),
                  onSetExpandedYear = year => $.modState(_.copy(expandedYear = year))
                )
              }
            }
          }.toVdomArray
        )
      }
    )
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(includeUnrelatedAccounts: Boolean,
                           query: String,
                           yearLowerBound: Int,
                           expandedYear: Int)
}
