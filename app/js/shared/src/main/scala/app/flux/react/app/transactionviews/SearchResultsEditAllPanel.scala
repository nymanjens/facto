package app.flux.react.app.transactionviews

import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.ComplexQueryStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import hydro.common.Formatting._
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.input.bootstrap.TextInput
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class SearchResultsEditAllPanel(implicit
    complexQueryStoreFactory: ComplexQueryStoreFactory,
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(query: String): VdomElement = {
    component(Props(query))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      val store = complexQueryStoreFactory.get(props.query, maxNumEntries = Int.MaxValue)
      StateStoresDependency(
        store,
        oldState => oldState.copy(maybeMatchingEntries = store.state.map(_.entries.map(_.entry))),
      )
    }

  // **************** Private inner types ****************//
  protected case class Props(
      query: String
  )
  protected case class State(
      maybeMatchingEntries: Option[Seq[GeneralEntry]] = None
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State) = {
      Panel(i18n("app.edit-all-results"))(
        state.maybeMatchingEntries match {
          case None => "..."
          case Some(matchingEntries) =>
            Bootstrap.Col(lg = 6)(
              Bootstrap.FormHorizontal(
                TextInput(
                  ref = TextInput.ref(),
                  name = "issuer",
                  label = i18n("app.issuer"),
                  defaultValue = "ABC",
                )
              )
            )
        }
      )
    }
  }
}
