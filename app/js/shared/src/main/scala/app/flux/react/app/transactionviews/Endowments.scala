package app.flux.react.app.transactionviews

import app.common.Formatting._
import app.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.EndowmentEntriesStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.Unique
import hydro.common.time.Clock
import hydro.flux.react.uielements.CollapseAllExpandAllButtons
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class Endowments(implicit entriesStoreFactory: EndowmentEntriesStoreFactory,
                       entityAccess: AppJsEntityAccess,
                       clock: Clock,
                       accountingConfig: Config,
                       user: User,
                       exchangeRateManager: ExchangeRateManager,
                       i18n: I18n,
                       pageHeader: PageHeader,
) {

  private val entriesListTable: EntriesListTable[GeneralEntry, Account] = new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(setExpanded = Unique(true)))
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          pageHeader.withExtension(router.currentPage) {
            CollapseAllExpandAllButtons(setExpanded => $.modState(_.copy(setExpanded = setExpanded)))
          },
          Panel(i18n("app.all-accounts")) {
            {
              for (account <- accountingConfig.personallySortedAccounts) yield {
                entriesListTable(
                  tableTitle = i18n("app.endowments-of", account.longName),
                  tableClasses = Seq("table-endowments"),
                  key = account.code,
                  numEntriesStrategy = NumEntriesStrategy(start = 30, intermediateBeforeInf = Seq(100)),
                  setExpanded = state.setExpanded,
                  additionalInput = account,
                  hideEmptyTable = true,
                  tableHeaders = Seq(
                    <.th(i18n("app.payed")),
                    <.th(i18n("app.consumed")),
                    <.th(i18n("app.beneficiary")),
                    <.th(i18n("app.payed-with-to")),
                    <.th(i18n("app.category")),
                    <.th(i18n("app.description")),
                    <.th(i18n("app.flow")),
                    <.th("")
                  ),
                  calculateTableData = entry =>
                    Seq[VdomElement](
                      <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                      <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                      <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                      <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                      <.td(entry.categories.map(_.name).mkString(", ")),
                      <.td(uielements.DescriptionWithEntryCount(entry)),
                      <.td(uielements.MoneyWithCurrency(entry.flow)),
                      <.td(uielements.TransactionGroupEditButton(entry.groupId))
                  )
                )
              }
            }.toVdomArray
          }
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
  private case class State(setExpanded: Unique[Boolean])
}
