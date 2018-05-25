package flux.react.app.transactionviews

import common.Formatting._
import common.money.{ExchangeRateManager, ReferenceMoney}
import common.time.Clock
import common.{I18n, Unique}
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.{Page, RouterContext}
import flux.react.uielements
import flux.stores.entries.factories.LiquidationEntriesStoreFactory
import flux.stores.entries.{AccountPair, LiquidationEntry}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.access.EntityAccess
import models.accounting.config.{Account, Config}
import models.user.User

import scala.collection.immutable.Seq

final class Liquidation(implicit entriesStoreFactory: LiquidationEntriesStoreFactory,
                        entityAccess: EntityAccess,
                        clock: Clock,
                        accountingConfig: Config,
                        user: User,
                        exchangeRateManager: ExchangeRateManager,
                        i18n: I18n) {

  private val entriesListTable: EntriesListTable[LiquidationEntry, AccountPair] = new EntriesListTable

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
          uielements.Panel(i18n("facto.all-combinations")) {
            {
              for {
                (account1, i1) <- accountingConfig.personallySortedAccounts.zipWithIndex
                (account2, i2) <- accountingConfig.personallySortedAccounts.zipWithIndex
                if i1 < i2
              } yield {
                val accountPair = AccountPair(account1, account2)
                val startNumEntries = 10
                entriesListTable(
                  tableTitle = i18n("facto.debt-of", account1.longName, account2.longName),
                  tableClasses = Seq("table-liquidation"),
                  key = s"${account1.code}_${account2.code}",
                  numEntriesStrategy =
                    NumEntriesStrategy(start = startNumEntries, intermediateBeforeInf = Seq(30)),
                  setExpanded = state.setExpanded,
                  additionalInput = accountPair,
                  latestEntryToTableTitleExtra = latestEntry => latestEntry.debt.toString,
                  tableHeaders = Seq(
                    <.th(i18n("facto.payed")),
                    <.th(i18n("facto.beneficiary")),
                    <.th(i18n("facto.payed-with-to")),
                    <.th(i18n("facto.category")),
                    <.th(i18n("facto.description")),
                    <.th(i18n("facto.flow")),
                    <.th(s"${account1.veryShortName} -> ${account2.veryShortName}"),
                    <.th(repayButton(account1 = account1, account2 = account2))
                  ),
                  calculateTableData = entry =>
                    Seq[VdomElement](
                      <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                      <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                      <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                      <.td(entry.categories.map(_.name).mkString(", ")),
                      <.td(uielements.DescriptionWithEntryCount(entry)),
                      <.td(uielements.MoneyWithCurrency(entry.flow)),
                      <.td(uielements.MoneyWithCurrency(entry.debt)),
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

  // **************** Private helper methods ****************//
  private def repayButton(account1: Account, account2: Account)(
      implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(Page.NewForRepayment(account1 = account1, account2 = account2))(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-check-square-o fa-fw"),
      " ",
      i18n("facto.repay")
    )

  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(setExpanded: Unique[Boolean])
}
