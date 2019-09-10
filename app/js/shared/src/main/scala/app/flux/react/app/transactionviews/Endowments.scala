package app.flux.react.app.transactionviews

import hydro.common.Formatting._
import hydro.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.CollapseAllExpandAllButtons
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.EndowmentEntriesStoreFactory
import app.flux.stores.CollapsedExpandedStateStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.Unique
import hydro.common.time.Clock
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class Endowments(
    implicit entriesStoreFactory: EndowmentEntriesStoreFactory,
    collapsedExpandedStateStoreFactory: CollapsedExpandedStateStoreFactory,
    entityAccess: AppJsEntityAccess,
    clock: Clock,
    accountingConfig: Config,
    user: User,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
) {

  private val entriesListTable: EntriesListTable[GeneralEntry, Account] = new EntriesListTable
  private val collapsedExpandedStateStoreHandle = collapsedExpandedStateStoreFactory
    .initializeView(getClass.getSimpleName, defaultExpanded = true)

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State())
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          pageHeader.withExtension(router.currentPage) {
            CollapseAllExpandAllButtons(
              onExpandedUpdate = collapsedExpandedStateStoreHandle.setExpandedForAllTables)
          },
          Panel(i18n("app.all-accounts")) {
            {
              for (account <- accountingConfig.personallySortedAccounts) yield {
                val tableName = account.code
                entriesListTable(
                  tableTitle = i18n("app.endowments-of", account.longName),
                  tableClasses = Seq("table-endowments"),
                  key = tableName,
                  numEntriesStrategy = NumEntriesStrategy(start = 30, intermediateBeforeInf = Seq(100)),
                  collapsedExpandedStateStore = Some(collapsedExpandedStateStoreHandle.getStore(tableName)),
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
                      <.td(descriptionWithEntryCount(entry)),
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
  private case class State()
}
