package app.flux.react.app.transactionviews

import scala.scalajs.js
import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.react.uielements.input.bootstrap.TagInput
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.ComplexQueryStoreFactory
import app.flux.stores.entries.factories.TagsStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.Formatting._
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.common.Tags
import hydro.flux.react.uielements.input.InputBase
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.input.bootstrap.TextInput
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.input.bootstrap.SelectInput
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class SearchResultsEditAllPanel(implicit
    complexQueryStoreFactory: ComplexQueryStoreFactory,
    entityAccess: AppJsEntityAccess,
    accountingConfig: Config,
    i18n: I18n,
    tagsStoreFactory: TagsStoreFactory,
) extends HydroReactComponent {

  private val operationSelectInput = SelectInput.forType[EditAllOperation]

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
    .withStateStoresDependency(
      tagsStoreFactory.get(),
      _.copy(allTags =
        tagsStoreFactory.get().state.map(_.tagToTransactionIds.keySet.toVector) getOrElse Seq()
      ),
    )

  // **************** Private inner types ****************//
  protected case class Props(
      query: String
  )
  protected case class State(
      showErrorMessages: Boolean = false,
      feedbackStatusMessage: String = "",
      editAllOperation: EditAllOperation = EditAllOperation.NoneSelected,
      maybeMatchingEntries: Option[Seq[GeneralEntry]] = None,
      allTags: Seq[String] = Seq(),
  )

  sealed abstract class EditAllOperation(val name: String)
  object EditAllOperation {
    def all: Seq[EditAllOperation] = Seq(NoneSelected, ChangeCategory, AddTag)

    object NoneSelected extends EditAllOperation("-----------")
    object ChangeCategory extends EditAllOperation(i18n("app.change-category"))
    object AddTag extends EditAllOperation(i18n("app.add-tag"))
  }

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    private val tagsRef = TagInput.ref()

    override def render(props: Props, state: State) = {
      Panel(i18n("app.edit-all-results"))(
        state.maybeMatchingEntries match {
          case None => "..."
          case Some(matchingEntries) =>
            Bootstrap.Col(lg = 6)(
              editAllForm(props, state)
            )
        }
      )
    }

    private def editAllForm(props: Props, state: State) = {
      val matchingEntries = state.maybeMatchingEntries.get

      Bootstrap.FormHorizontal(
        TextInput(
          ref = TextInput.ref(),
          name = "query",
          label = i18n("app.query"),
          defaultValue = props.query,
          disabled = true,
        ),
        Bootstrap.Row(
          ^.style := js.Dictionary("paddingBottom" -> "15px"),
          Bootstrap.Col(sm = 4)(<.span()),
          Bootstrap.Col(sm = 8) {
            i18n(
              "app.matching-n-grouped-entries-m-individual-entries",
              matchingEntries.size,
              matchingEntries.flatMap(_.transactions).size,
            )
          },
        ),
        operationSelectInput(
          ref = operationSelectInput.ref(),
          name = "operation",
          label = i18n("app.operation"),
          defaultValue = state.editAllOperation,
          options = EditAllOperation.all,
          valueToId = _.getClass.getSimpleName,
          valueToName = _.name,
          listener = OperationListener,
        ),
        state.editAllOperation match {
          case EditAllOperation.NoneSelected => <.span()
          case EditAllOperation.ChangeCategory =>
            <.span()
          case EditAllOperation.AddTag =>
            TagInput(
              ref = tagsRef,
              name = "tag",
              label = i18n("app.tag"),
              suggestions = state.allTags,
              showErrorMessage = state.showErrorMessages,
              additionalValidator = tags => tags.size == 1 && tags.forall(Tags.isValidTag),
              defaultValue = Seq(),
            )
        },
        TextInput(
          ref = TextInput.ref(),
          name = "issuer",
          label = i18n("app.issuer"),
          defaultValue = "ABC",
        ),
      )
    }

    private object OperationListener extends InputBase.Listener[EditAllOperation] {
      override def onChange(newOperation: EditAllOperation, directUserChange: Boolean) = {
        $.modState(_.copy(editAllOperation = newOperation))
      }
    }
  }
}
