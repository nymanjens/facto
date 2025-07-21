package app.flux.react.app.transactionviews

import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.common.Formatting._
import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.flux.action.AppActions
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.CollapseAllExpandAllButtons
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.router.AppPages
import app.flux.stores.entries.CashFlowEntry
import app.flux.stores.entries.factories.CashFlowEntriesStoreFactory
import app.flux.stores.CollapsedExpandedStateStoreFactory
import app.flux.stores.InMemoryUserConfigStore
import app.flux.stores.entries.factories.CashFlowEntriesStoreFactory.CashFlowAdditionalInput
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
import hydro.flux.react.HydroReactComponent
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
    currencyValueManager: CurrencyValueManager,
    i18n: I18n,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
    inMemoryUserConfigStore: InMemoryUserConfigStore,
) extends HydroReactComponent {

  private val entriesListTable: EntriesListTable[CashFlowEntry, CashFlowAdditionalInput] =
    new EntriesListTable
  private val collapsedExpandedStateStoreHandle = collapsedExpandedStateStoreFactory
    .initializeView(getClass.getSimpleName, defaultExpanded = user.expandCashFlowTablesByDefault)

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      inMemoryUserConfigStore,
      _.copy(correctForInflation = inMemoryUserConfigStore.state.correctForInflation),
    )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      router: RouterContext
  )
  protected case class State(
      includeUnrelatedReservoirs: Boolean = false,
      includeHiddenReservoirs: Boolean = false,
      correctForInflation: Boolean = false,
      private val reservoirsThatShowAllBalanceChecks: Set[MoneyReservoir] = Set(),
  ) {
    def showAllBalanceChecks(reservoir: MoneyReservoir): Boolean = {
      reservoirsThatShowAllBalanceChecks contains reservoir
    }

    def withToggledShowAllBalanceChecks(reservoir: MoneyReservoir): State = {
      if (reservoirsThatShowAllBalanceChecks contains reservoir) {
        copy(reservoirsThatShowAllBalanceChecks = reservoirsThatShowAllBalanceChecks - reservoir)
      } else {
        copy(reservoirsThatShowAllBalanceChecks = reservoirsThatShowAllBalanceChecks + reservoir)
      }
    }
  }

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State) = {
      implicit val router = props.router
      implicit val _ = state

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
                    additionalInput = CashFlowAdditionalInput(
                      reservoir = reservoir,
                      showAllBalanceChecks = {
                        state.showAllBalanceChecks(reservoir)
                      },
                    ),
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
                      <.th(
                        i18n("app.balance"),
                        " ",
                        balanceCheckAddNewButton(reservoir),
                        " ",
                        toggleShowAllBalanceChecks(reservoir),
                      ),
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
                            <.td(
                              uielements.MoneyWithCurrency.sum(
                                entry.flows,
                                markPositiveFlow = true,
                                correctForInflation = state.correctForInflation,
                              )
                            ),
                            <.td(
                              uielements.MoneyWithCurrency(
                                entry.balance,
                                correctForInflation = state.correctForInflation,
                              ),
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
                              if (entry.balanceIncrease.cents >= 0) "+" else "-",
                              " ",
                              uielements.MoneyWithCurrency(
                                if (entry.balanceIncrease.cents >= 0) entry.balanceIncrease
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
        Bootstrap.Row(
          Bootstrap.Col(sm = 6)(
            // includeUnrelatedReservoirs toggle button
            Bootstrap.Button(Variant.info, Size.lg, block = true, tag = <.a)(
              ^.onClick --> LogExceptionsCallback(
                $.modState(s => s.copy(includeUnrelatedReservoirs = !s.includeUnrelatedReservoirs)).runNow()
              ),
              if (state.includeUnrelatedReservoirs) i18n("app.hide-other-accounts")
              else i18n("app.show-other-accounts"),
            )
          ),
          Bootstrap.Col(sm = 6)(
            // includeHiddenReservoirs toggle button
            Bootstrap.Button(Variant.info, Size.lg, block = true, tag = <.a)(
              ^.onClick --> LogExceptionsCallback(
                $.modState(s => s.copy(includeHiddenReservoirs = !s.includeHiddenReservoirs)).runNow()
              ),
              if (state.includeHiddenReservoirs) i18n("app.hide-hidden-reservoirs")
              else i18n("app.show-hidden-reservoirs"),
            )
          ),
        ),
      )
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

    private def toggleShowAllBalanceChecks(
        reservoir: MoneyReservoir
    )(implicit router: RouterContext, state: State): VdomElement = {
      Bootstrap.Button(size = Size.xs)(
        Bootstrap.Glyphicon(
          if (state.showAllBalanceChecks(reservoir)) "zoom-out" else "zoom-in"
        ),
        ^.onClick --> LogExceptionsCallback(
          $.modState(_.withToggledShowAllBalanceChecks(reservoir)).runNow()
        ),
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

    private def balanceCheckConfirmButton(reservoir: MoneyReservoir, entry: CashFlowEntry.RegularEntry)(
        implicit router: RouterContext
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

    def balanceCheckEditButton(
        balanceCorrection: BalanceCheck
    )(implicit router: RouterContext): VdomElement = {
      val link = router.anchorWithHrefTo(AppPages.EditBalanceCheck(balanceCorrection))
      Bootstrap.Button(size = Size.xs, tag = link)(
        Bootstrap.FontAwesomeIcon("pencil", fixedWidth = true)
      )
    }
  }
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
