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

final class Summary(implicit summaryForYearStoreFactory: SummaryForYearStoreFactory,
                    summaryYearsStoreFactory: SummaryYearsStoreFactory,
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
        hideColumnsOlderThanYear = clock.now.getYear - 1,
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
                <.span(state.query)
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
                           hideColumnsOlderThanYear: Int,
                           expandedYear: Int)
}
