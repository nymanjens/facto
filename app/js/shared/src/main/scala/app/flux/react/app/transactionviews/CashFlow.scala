package app.flux.react.app.transactionviews

import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.common.Formatting._
import hydro.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.action.AppActions
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.CollapseAllExpandAllButtons
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.router.AppPages
import app.flux.stores.entries.CashFlowEntry
import app.flux.stores.entries.factories.CashFlowEntriesStoreFactory
import app.flux.stores.CollapsedExpandedStateStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.BalanceCheck
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.user.User
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

final class CashFlow(implicit
    entriesStoreFactory: CashFlowEntriesStoreFactory,
    collapsedExpandedStateStoreFactory: CollapsedExpandedStateStoreFactory,
    dispatcher: Dispatcher,
    entityAccess: AppJsEntityAccess,
    clock: Clock,
    accountingConfig: Config,
    user: User,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
) {

  private val entriesListTable: EntriesListTable[CashFlowEntry, MoneyReservoir] = new EntriesListTable
  private val collapsedExpandedStateStoreHandle = collapsedExpandedStateStoreFactory
    .initializeView(getClass.getSimpleName, defaultExpanded = user.expandCashFlowTablesByDefault)

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State(includeUnrelatedReservoirs = false, includeHiddenReservoirs = false))
    .renderPS(($, props, state) => {
      implicit val router = props.router
      <.span(
        pageHeader.withExtension(router.currentPage) {
          CollapseAllExpandAllButtons(
            onExpandedUpdate = collapsedExpandedStateStoreHandle.setExpandedForAllTables
          )
        },
        (
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
                      includeHidden = state.includeHiddenReservoirs,
                    )
                  if reservoir.owner == account
                } yield {
                  val tableName = reservoir.code
                  entriesListTable.withRowNumber(
                    tableTitle = reservoir.name,
                    tableClasses = Seq("table-cashflow"),
                    key = tableName,
                    numEntriesStrategy = NumEntriesStrategy(
                      start = CashFlow.minNumEntriesPerReservoir,
                      intermediateBeforeInf = Seq(150),
                    ),
                    collapsedExpandedStateStore = Some(collapsedExpandedStateStoreHandle.getStore(tableName)),
                    additionalInput = reservoir,
                    calculateExtraTitle = { context =>
                      context.maybeLatestEntry map { latestEntry =>
                        s"${i18n("app.balance")}: ${latestEntry.balance}" + (
                          if (reservoir.currency.isForeign)
                            s" (${latestEntry.balance.withDate(clock.now).exchangedForReferenceCurrency()})"
                          else ""
                        )
                      }
                    },
                    tableHeaders = Seq(
                      <.th(i18n("app.payed")),
                      <.th(i18n("app.consumed")),
                      <.th(i18n("app.beneficiary")),
                      <.th(i18n("app.category")),
                      <.th(i18n("app.description")),
                      <.th(i18n("app.flow")),
                      <.th(i18n("app.balance"), " ", balanceCheckAddNewButton(reservoir)),
                      <.th(transactionGroupAddButton(reservoir)),
                    ),
                    calculateTableDataFromEntryAndRowNum = (cashFlowEntry, rowNumber) =>
                      cashFlowEntry match {
                        case entry: CashFlowEntry.RegularEntry =>
                          Seq[VdomElement](
                            <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                            <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                            <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                            <.td(entry.categories.map(_.name).mkString(", ")),
                            <.td(descriptionWithEntryCount(entry)),
                            <.td(uielements.MoneyWithCurrency(entry.flow)),
                            <.td(
                              uielements.MoneyWithCurrency(entry.balance),
                              entry.balanceVerified match {
                                case true =>
                                  <.span(" ", Bootstrap.FontAwesomeIcon("check", fixedWidth = true))
                                case false if rowNumber == 0 =>
                                  <.span(" ", balanceCheckConfirmButton(reservoir, entry))
                                case _ => VdomArray.empty()
                              },
                            ),
                            <.td(
                              uielements.TransactionGroupEditButtons(
                                entry.groupId
                              )
                            ),
                          )
                        case entry @ CashFlowEntry.BalanceCorrection(balanceCorrection, expectedAmount) =>
                          Seq[VdomElement](
                            <.td(formatDate(balanceCorrection.checkDate)),
                            <.td(
                              ^.colSpan := 4,
                              ^.style := js.Dictionary("fontWeight" -> "bold"),
                              i18n("app.balance-correction") + ":",
                            ),
                            <.td(
                              ^.style := js.Dictionary("fontWeight" -> "bold"),
                              if (entry.balanceIncrease.cents > 0) "+" else "-",
                              " ",
                              uielements.MoneyWithCurrency(
                                if (entry.balanceIncrease.cents > 0) entry.balanceIncrease
                                else -entry.balanceIncrease
                              ),
                            ),
                            <.td(
                              ^.style := js.Dictionary("fontWeight" -> "bold"),
                              uielements.MoneyWithCurrency(balanceCorrection.balance),
                            ),
                            <.td(balanceCheckEditButton(balanceCorrection)),
                          )
                      },
                  )
                }
              }.toVdomArray
            }
          }
        ).toVdomArray,
        // includeUnrelatedReservoirs toggle button
        Bootstrap.Button(Variant.info, Size.lg, block = true, tag = <.a)(
          ^.onClick --> LogExceptionsCallback(
            $.modState(s => s.copy(includeUnrelatedReservoirs = !s.includeUnrelatedReservoirs)).runNow()
          ),
          if (state.includeUnrelatedReservoirs) i18n("app.hide-other-accounts")
          else i18n("app.show-other-accounts"),
        ),
        // includeHiddenReservoirs toggle button
        <<.ifThen(state.includeHiddenReservoirs || state.includeUnrelatedReservoirs) {
          Bootstrap.Button(Variant.info, Size.lg, block = true, tag = <.a)(
            ^.onClick --> LogExceptionsCallback(
              $.modState(s => s.copy(includeHiddenReservoirs = !s.includeHiddenReservoirs)).runNow()
            ),
            if (state.includeHiddenReservoirs) i18n("app.hide-hidden-reservoirs")
            else i18n("app.show-hidden-reservoirs"),
          )
        },
      )
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private helper methods ****************//
  private def balanceCheckAddNewButton(
      reservoir: MoneyReservoir
  )(implicit router: RouterContext): VdomElement = {
    val link = router.anchorWithHrefTo(AppPages.NewBalanceCheck(reservoir))
    Bootstrap.Button(Variant.info, Size.xs, tag = link)(
      Bootstrap.FontAwesomeIcon("check-square-o", fixedWidth = true)
    )
  }
  private def transactionGroupAddButton(
      reservoir: MoneyReservoir
  )(implicit router: RouterContext): VdomElement = {
    val link = router.anchorWithHrefTo(AppPages.NewTransactionGroupFromReservoir(reservoir))
    Bootstrap.Button(Variant.info, Size.xs, tag = link)(
      <.i(^.className := "icon-new-empty")
    )
  }

  private def balanceCheckConfirmButton(reservoir: MoneyReservoir, entry: CashFlowEntry.RegularEntry)(implicit
      router: RouterContext
  ): VdomElement = {
    Bootstrap.Button(Variant.info, Size.xs)(
      ^.onClick --> LogExceptionsCallback {
        dispatcher.dispatch(
          AppActions.AddBalanceCheck(
            BalanceCheck(
              issuerId = user.id,
              moneyReservoirCode = reservoir.code,
              balanceInCents = entry.balance.cents,
              createdDate = clock.now,
              checkDate = entry.mostRecentTransaction.transactionDate,
            )
          )
        )
      }.void,
      Bootstrap.FontAwesomeIcon("check-square-o", fixedWidth = true),
    )
  }

  def balanceCheckEditButton(balanceCorrection: BalanceCheck)(implicit router: RouterContext): VdomElement = {
    val link = router.anchorWithHrefTo(AppPages.EditBalanceCheck(balanceCorrection))
    Bootstrap.Button(size = Size.xs, tag = link)(
      Bootstrap.FontAwesomeIcon("pencil", fixedWidth = true)
    )
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(
      includeUnrelatedReservoirs: Boolean,
      includeHiddenReservoirs: Boolean,
  )
}

object CashFlow {

  def minNumEntriesPerReservoir(implicit user: User): Int = {
    if (user.expandCashFlowTablesByDefault) {
      10
    } else {
      35
    }
  }
}
