package app.flux.react.app.transactionviews

import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.react.uielements.input.TextInput
import hydro.common.Formatting._
import hydro.common.I18n
import app.common.money.CurrencyValueManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.ComplexQueryStoreFactory
import app.flux.stores.InMemoryUserConfigStore
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import hydro.common.time.Clock
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

import scala.scalajs.js

final class SearchResults(implicit
    complexQueryStoreFactory: ComplexQueryStoreFactory,
    entityAccess: AppJsEntityAccess,
    clock: Clock,
    accountingConfig: Config,
    currencyValueManager: CurrencyValueManager,
    i18n: I18n,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
    searchResultsEditAllPanel: SearchResultsEditAllPanel,
    inMemoryUserConfigStore: InMemoryUserConfigStore,
) extends HydroReactComponent {

  private val entriesListTable: EntriesListTable[GeneralEntry, ComplexQueryStoreFactory.Query] =
    new EntriesListTable

  // **************** API ****************//
  def apply(query: String, router: RouterContext): VdomElement = {
    component(Props(query, router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      inMemoryUserConfigStore,
      _.copy(correctForInflation = inMemoryUserConfigStore.state.correctForInflation),
    )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      query: String,
      router: RouterContext,
  )
  protected case class State(
      showUpdateAllPanel: Boolean = false,
      correctForInflation: Boolean = false,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    private val queryInputRef = TextInput.ref()

    override def render(props: Props, state: State) = {
      implicit val router = props.router
      <.span(
        pageHeader.withExtension(router.currentPage)(
          <.span(
            Bootstrap.Button(variant = Variant.default)(
              ^.onClick --> $.modState(state => state.copy(showUpdateAllPanel = !state.showUpdateAllPanel)),
              Bootstrap.FontAwesomeIcon("magic"),
              " ",
              if (state.showUpdateAllPanel) {
                i18n("app.hide-edit-all-results-panel")
              } else {
                i18n("app.edit-all-results")
              },
            )
          )
        ),
        wideSearchBar(props.query),
        <<.ifThen(state.showUpdateAllPanel)(searchResultsEditAllPanel(props.query)),
        Panel(i18n("app.search-results"))(
          entriesListTable(
            tableTitle = i18n("app.all"),
            tableClasses = Seq("table-search"),
            numEntriesStrategy = NumEntriesStrategy(start = 500),
            additionalInput = props.query,
            calculateExtraTitle = { context =>
              val totalFlow = context.entriesInChronologicalOrder
                .flatMap(_.transactions)
                .map(_.flow.exchangedForReferenceCurrency())
                .sum
              Some(s"${i18n("app.total")}: $totalFlow")
            },
            tableHeaders = Seq(
              <.th(i18n("app.issuer")),
              <.th(i18n("app.payed")),
              <.th(i18n("app.consumed")),
              <.th(i18n("app.beneficiary")),
              <.th(i18n("app.payed-with-to")),
              <.th(i18n("app.category")),
              <.th(i18n("app.description")),
              <.th(i18n("app.flow")),
              <.th(""),
            ),
            calculateTableData = entry =>
              Seq[VdomElement](
                <.td(entry.issuer.name),
                <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                <.td(entry.categories.map(_.name).mkString(", ")),
                <.td(descriptionWithEntryCount(entry)),
                <.td(
                  uielements.MoneyWithCurrency
                    .sum(
                      entry.flows,
                      markPositiveFlow = true,
                      correctForInflation = state.correctForInflation,
                    )
                ),
                <.td(uielements.TransactionGroupEditButtons(entry.groupId)),
              ),
          )
        ),
      )
    }

    private def wideSearchBar(query: String)(implicit router: RouterContext) = {
      Bootstrap.Row(
        ^.style := js.Dictionary(
          "paddingLeft" -> "20px",
          "paddingRight" -> "20px",
          "paddingBottom" -> "20px",
        ),
        <.form(
          Bootstrap.InputGroup(
            ^.className := "custom-search-form",
            TextInput(
              ref = queryInputRef,
              name = "query",
              defaultValue = query,
              classes = Seq("form-control"),
            ),
            Bootstrap.InputGroupButton(
              Bootstrap.Button(variant = Variant.default, tpe = "submit")(
                ^.onClick ==> { (e: ReactEventFromInput) =>
                  LogExceptionsCallback {
                    e.preventDefault()

                    queryInputRef().value match {
                      case Some(query) => router.setPage(StandardPages.Search.fromInput(query))
                      case None        =>
                    }
                  }
                },
                Bootstrap.FontAwesomeIcon("search"),
              )
            ),
          )
        ),
      )
    }
  }
}
