package app.flux.react.app.transactionviews

import common.Formatting._
import common.money.ExchangeRateManager
import common.time.Clock
import common.I18n
import common.Unique
import app.flux.react.ReactVdomUtils.^^
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.router.Page.NewForLiquidationSimplification
import app.flux.router.Page
import app.flux.router.RouterContext
import app.flux.react.uielements
import app.flux.stores.entries.factories.LiquidationEntriesStoreFactory
import app.flux.stores.entries.AccountPair
import app.flux.stores.entries.LiquidationEntry
import hydro.flux.react.uielements.CollapseAllExpandAllButtons
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import app.models.access.EntityAccess
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.user.User

import scala.collection.immutable.Seq

final class Liquidation(implicit entriesStoreFactory: LiquidationEntriesStoreFactory,
                        entityAccess: EntityAccess,
                        clock: Clock,
                        accountingConfig: Config,
                        user: User,
                        exchangeRateManager: ExchangeRateManager,
                        i18n: I18n,
                        pageHeader: PageHeader,
) {

  private val entriesListTable: EntriesListTable[LiquidationEntry, AccountPair] = new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(setExpanded = Unique(user.expandLiquidationTablesByDefault)))
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          pageHeader.withExtension(router.currentPage)(
            CollapseAllExpandAllButtons(setExpanded => $.modState(_.copy(setExpanded = setExpanded))),
            " ",
            simplifyLiquidationButton()
          ),
          Panel(i18n("app.all-combinations")) {
            {
              for {
                (account1, i1) <- accountingConfig.personallySortedAccounts.zipWithIndex
                (account2, i2) <- accountingConfig.personallySortedAccounts.zipWithIndex
                if i1 < i2
              } yield {
                val accountPair = AccountPair(account1, account2)
                entriesListTable(
                  tableTitle = i18n("app.debt-of", account1.longName, account2.longName),
                  tableClasses = Seq("table-liquidation"),
                  key = s"${account1.code}_${account2.code}",
                  numEntriesStrategy = NumEntriesStrategy(
                    start = Liquidation.minNumEntriesPerPair,
                    intermediateBeforeInf = Seq(30)),
                  setExpanded = state.setExpanded,
                  additionalInput = accountPair,
                  latestEntryToTableTitleExtra = latestEntry => latestEntry.debt.toString,
                  hideEmptyTable = true,
                  tableHeaders = Seq(
                    <.th(i18n("app.payed")),
                    <.th(i18n("app.beneficiary")),
                    <.th(i18n("app.payed-with-to")),
                    <.th(i18n("app.category")),
                    <.th(i18n("app.description")),
                    <.th(i18n("app.flow")),
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
  private def simplifyLiquidationButton()(implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(Page.NewForLiquidationSimplification())(
      ^^.classes("btn", "btn-default"),
      ^.role := "button",
      <.i(^.className := "fa fa-scissors fa-fw"),
      " ",
      i18n("app.simplify-liquidation")
    )
  }

  private def repayButton(account1: Account, account2: Account)(
      implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(Page.NewForRepayment(account1 = account1, account2 = account2))(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-check-square-o fa-fw"),
      " ",
      i18n("app.repay")
    )
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(setExpanded: Unique[Boolean])
}

object Liquidation {
  val minNumEntriesPerPair: Int = 10
}
