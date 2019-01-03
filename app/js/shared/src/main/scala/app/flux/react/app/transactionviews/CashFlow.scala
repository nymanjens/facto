package app.flux.react.app.transactionviews

import hydro.common.Formatting._
import hydro.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.action.AppActions
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.router.AppPages
import app.flux.stores.entries.CashFlowEntry
import app.flux.stores.entries.factories.CashFlowEntriesStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.BalanceCheck
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.user.User
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.Unique
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.CollapseAllExpandAllButtons
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

final class CashFlow(implicit entriesStoreFactory: CashFlowEntriesStoreFactory,
                     dispatcher: Dispatcher,
                     entityAccess: AppJsEntityAccess,
                     clock: Clock,
                     accountingConfig: Config,
                     user: User,
                     exchangeRateManager: ExchangeRateManager,
                     i18n: I18n,
                     pageHeader: PageHeader,
) {

  private val entriesListTable: EntriesListTable[CashFlowEntry, MoneyReservoir] = new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(
      State(
        includeUnrelatedReservoirs = false,
        includeHiddenReservoirs = false,
        setExpanded = Unique(user.expandCashFlowTablesByDefault)))
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          pageHeader.withExtension(router.currentPage) {
            CollapseAllExpandAllButtons(setExpanded => $.modState(_.copy(setExpanded = setExpanded)))
          }, {
            for {
              account <- accountingConfig.personallySortedAccounts
              if state.includeUnrelatedReservoirs || account.isMineOrCommon
            } yield {
              Panel(i18n("app.account-of", account.longName), key = account.code) {
                {
                  for {
                    reservoir <- accountingConfig
                      .moneyReservoirs(
                        includeNullReservoir = false,
                        includeHidden = state.includeHiddenReservoirs)
                    if reservoir.owner == account
                  } yield {
                    entriesListTable.withRowNumber(
                      tableTitle = reservoir.name,
                      tableClasses = Seq("table-cashflow"),
                      key = reservoir.code,
                      numEntriesStrategy = NumEntriesStrategy(
                        start = CashFlow.minNumEntriesPerReservoir,
                        intermediateBeforeInf = Seq(150)),
                      setExpanded = state.setExpanded,
                      additionalInput = reservoir,
                      latestEntryToTableTitleExtra =
                        latestEntry => s"${i18n("app.balance")}: ${latestEntry.balance}",
                      tableHeaders = Seq(
                        <.th(i18n("app.payed")),
                        <.th(i18n("app.consumed")),
                        <.th(i18n("app.beneficiary")),
                        <.th(i18n("app.category")),
                        <.th(i18n("app.description")),
                        <.th(i18n("app.flow")),
                        <.th(i18n("app.balance"), " ", balanceCheckAddNewButton(reservoir)),
                        <.th(transactionGroupAddButton(reservoir))
                      ),
                      calculateTableDataFromEntryAndRowNum = (cashFlowEntry, rowNumber) =>
                        cashFlowEntry match {
                          case entry: CashFlowEntry.RegularEntry =>
                            Seq[VdomElement](
                              <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                              <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                              <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                              <.td(entry.categories.map(_.name).mkString(", ")),
                              <.td(uielements.DescriptionWithEntryCount(entry)),
                              <.td(uielements.MoneyWithCurrency(entry.flow)),
                              <.td(
                                uielements.MoneyWithCurrency(entry.balance),
                                entry.balanceVerified match {
                                  case true => <.span(" ", <.i(^.className := "fa fa-check fa-fw"))
                                  case false if rowNumber == 0 =>
                                    <.span(" ", balanceCheckConfirmButton(reservoir, entry))
                                  case _ => VdomArray.empty()
                                }
                              ),
                              <.td(uielements.TransactionGroupEditButton(entry.groupId))
                            )
                          case entry @ CashFlowEntry.BalanceCorrection(balanceCorrection, expectedAmount) =>
                            Seq[VdomElement](
                              <.td(formatDate(balanceCorrection.checkDate)),
                              <.td(
                                ^.colSpan := 4,
                                ^.style := js.Dictionary("fontWeight" -> "bold"),
                                i18n("app.balance-correction") + ":"),
                              <.td(
                                ^.style := js.Dictionary("fontWeight" -> "bold"),
                                if (entry.balanceIncrease.cents > 0) "+" else "-",
                                " ",
                                uielements.MoneyWithCurrency(
                                  if (entry.balanceIncrease.cents > 0) entry.balanceIncrease
                                  else -entry.balanceIncrease)
                              ),
                              <.td(
                                ^.style := js.Dictionary("fontWeight" -> "bold"),
                                uielements.MoneyWithCurrency(balanceCorrection.balance)),
                              <.td(balanceCheckEditButton(balanceCorrection))
                            )
                      }
                    )
                  }
                }.toVdomArray
              }
            }
          }.toVdomArray,
          // includeUnrelatedReservoirs toggle button
          <.a(
            ^.className := "btn btn-info btn-lg btn-block",
            ^.onClick --> LogExceptionsCallback(
              $.modState(s => s.copy(includeUnrelatedReservoirs = !s.includeUnrelatedReservoirs)).runNow()),
            if (state.includeUnrelatedReservoirs) i18n("app.hide-other-accounts")
            else i18n("app.show-other-accounts")
          ),
          // includeHiddenReservoirs toggle button
          <<.ifThen(state.includeHiddenReservoirs || state.includeUnrelatedReservoirs) {
            <.a(
              ^.className := "btn btn-info btn-lg btn-block",
              ^.onClick --> LogExceptionsCallback(
                $.modState(s => s.copy(includeHiddenReservoirs = !s.includeHiddenReservoirs)).runNow()),
              if (state.includeHiddenReservoirs) i18n("app.hide-hidden-reservoirs")
              else i18n("app.show-hidden-reservoirs")
            )
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
  private def balanceCheckAddNewButton(reservoir: MoneyReservoir)(
      implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(AppPages.NewBalanceCheck(reservoir))(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-check-square-o fa-fw")
    )
  }
  private def transactionGroupAddButton(reservoir: MoneyReservoir)(
      implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(AppPages.NewTransactionGroupFromReservoir(reservoir))(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "icon-new-empty"),
      " ",
      i18n("app.add-new")
    )
  }

  private def balanceCheckConfirmButton(reservoir: MoneyReservoir, entry: CashFlowEntry.RegularEntry)(
      implicit router: RouterContext): VdomElement = {
    <.button(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      ^.onClick --> LogExceptionsCallback {
        dispatcher.dispatch(
          AppActions.AddBalanceCheck(BalanceCheck(
            issuerId = user.id,
            moneyReservoirCode = reservoir.code,
            balanceInCents = entry.balance.cents,
            createdDate = clock.now,
            checkDate = entry.mostRecentTransaction.transactionDate
          )))
      }.void,
      <.i(^.className := "fa fa-check-square-o fa-fw")
    )
  }

  def balanceCheckEditButton(balanceCorrection: BalanceCheck)(implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(AppPages.EditBalanceCheck(balanceCorrection))(
      ^.className := "btn btn-default btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-pencil fa-fw"),
      " ",
      i18n("app.edit")
    )
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(includeUnrelatedReservoirs: Boolean,
                           includeHiddenReservoirs: Boolean,
                           setExpanded: Unique[Boolean])
}

object CashFlow {
  val minNumEntriesPerReservoir: Int = 10
}
