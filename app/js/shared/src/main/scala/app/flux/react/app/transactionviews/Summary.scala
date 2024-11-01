package app.flux.react.app.transactionviews

import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.common.time.AccountingYear
import app.common.time.DatedMonth
import app.flux.stores.InMemoryUserConfigStore
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.HydroReactComponent
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class Summary(implicit
    summaryTable: SummaryTable,
    entityAccess: AppJsEntityAccess,
    user: User,
    clock: Clock,
    accountingConfig: Config,
    currencyValueManager: CurrencyValueManager,
    i18n: I18n,
    pageHeader: PageHeader,
    inMemoryUserConfigStore: InMemoryUserConfigStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(
    backendConstructor = new Backend(_),
    initialState = State(
      yearLowerBound = DatedMonth.current.accountingYear.plusYears(-1),
      expandedYear = DatedMonth.current.accountingYear,
    ),
  ).withStateStoresDependency(
    inMemoryUserConfigStore,
    _.copy(correctForInflation = inMemoryUserConfigStore.state.correctForInflation),
  )

  // **************** Private inner types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      includeUnrelatedAccounts: Boolean = false,
      query: String = "",
      yearLowerBound: AccountingYear,
      expandedYear: AccountingYear,
      showYearlyTotal: Boolean = false, // instead of average
      correctForInflation: Boolean = false,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    private val queryInputRef = TextInput.ref()

    override def render(props: Props, state: State) = logExceptions {
      implicit val router = props.router
      <.span(
        pageHeader.withExtension(router.currentPage)(
          Bootstrap.FormInline()(
            ^.className := "summary-query-filter",
            Bootstrap.InputGroup(
              TextInput(
                ref = queryInputRef,
                name = "query",
                placeholder = i18n("app.example-query"),
                classes = Seq("form-control"),
              ),
              Bootstrap.InputGroupButton(
                Bootstrap.Button(tpe = "submit")(
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    LogExceptionsCallback {
                      e.preventDefault()
                      $.modState(_.copy(query = queryInputRef().value getOrElse "")).runNow()
                    }
                  },
                  Bootstrap.FontAwesomeIcon("search"),
                )
              ),
            ),
          )
        ), {
          for {
            account <- accountingConfig.personallySortedAccounts
            if state.includeUnrelatedAccounts || account.isMineOrCommon
          } yield {
            summaryTable(
              key = account.code,
              account = account,
              query = state.query,
              yearLowerBound = state.yearLowerBound,
              expandedYear = state.expandedYear,
              showYearlyTotal = state.showYearlyTotal,
              onShowHiddenYears = $.modState(_.copy(yearLowerBound = AccountingYear(Int.MinValue))),
              onSetExpandedYear = year => $.modState(_.copy(expandedYear = year)),
              onShowYearlyTotalToggle = $.modState(s => s.copy(showYearlyTotal = !s.showYearlyTotal)),
              correctForInflation = state.correctForInflation,
            )
          }
        }.toVdomArray,
        // includeUnrelatedAccounts toggle button
        Bootstrap.Button(Variant.info, Size.lg, block = true, tag = <.a)(
          ^.onClick --> $.modState(s => s.copy(includeUnrelatedAccounts = !s.includeUnrelatedAccounts)),
          if (state.includeUnrelatedAccounts) i18n("app.hide-other-accounts")
          else i18n("app.show-other-accounts"),
        ),
      )
    }
  }
}
