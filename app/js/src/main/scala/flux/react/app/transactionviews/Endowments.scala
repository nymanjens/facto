package flux.react.app.transactionviews

import common.Formatting._
import common.money.ExchangeRateManager
import common.time.Clock
import common.{I18n, Unique}
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.RouterContext
import flux.react.uielements
import flux.stores.entries.GeneralEntry
import flux.stores.entries.factories.EndowmentEntriesStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess
import models.accounting.config.{Account, Config}
import models.user.User

import scala.collection.immutable.Seq

final class Endowments(implicit entriesStoreFactory: EndowmentEntriesStoreFactory,
                       entityAccess: EntityAccess,
                       clock: Clock,
                       accountingConfig: Config,
                       user: User,
                       exchangeRateManager: ExchangeRateManager,
                       i18n: I18n) {

  private val entriesListTable: EntriesListTable[GeneralEntry, Account] = new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(setExpanded = Unique(true)))
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          uielements.PageHeader.withExtension(router.currentPage) {
            uielements.CollapseAllExpandAllButtons(setExpanded =>
              $.modState(_.copy(setExpanded = setExpanded)))
          },
          uielements.Panel(i18n("facto.all-accounts")) {
            {
              for (account <- accountingConfig.personallySortedAccounts) yield {
                entriesListTable(
                  tableTitle = i18n("facto.endowments-of", account.longName),
                  tableClasses = Seq("table-endowments"),
                  key = account.code,
                  numEntriesStrategy = NumEntriesStrategy(start = 30, intermediateBeforeInf = Seq(100)),
                  setExpanded = state.setExpanded,
                  additionalInput = account,
                  tableHeaders = Seq(
                    <.th(i18n("facto.payed")),
                    <.th(i18n("facto.consumed")),
                    <.th(i18n("facto.beneficiary")),
                    <.th(i18n("facto.payed-with-to")),
                    <.th(i18n("facto.category")),
                    <.th(i18n("facto.description")),
                    <.th(i18n("facto.flow")),
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
