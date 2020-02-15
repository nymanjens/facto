package hydro.flux.react.uielements.dbexplorer

import app.models.access.ModelFields
import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Table
import hydro.flux.react.uielements.Table.TableRowData
import hydro.flux.stores.DatabaseExplorerStoreFactory
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityType
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

private[dbexplorer] final class DatabaseTableView(
    implicit jsEntityAccess: JsEntityAccess,
    i18n: I18n,
    databaseExplorerStoreFactory: DatabaseExplorerStoreFactory,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(entityType: EntityType.any): VdomElement = {
    component(Props(entityType = entityType))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps { props =>
      val store = databaseExplorerStoreFactory.get(props.entityType)
      StateStoresDependency(store, _.copy(maybeStoreState = store.state))
    }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(entityType: EntityType.any)
  protected case class State(
      expanded: Boolean = false,
      maybeStoreState: Option[DatabaseExplorerStoreFactory.State] = None,
  )

  protected final class Backend(val $ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State) = logExceptions {
      implicit val _ = props
      implicit val __ = state
      Table(
        title = props.entityType.entityClass.getSimpleName,
        tableClasses = Seq(),
        expanded = state.expanded,
        onToggleCollapsedExpanded = Some(() => $.modState(s => s.copy(expanded = !s.expanded)).runNow()),
        tableTitleExtra = <<.ifDefined(state.maybeStoreState) { storeState =>
          s"(${storeState.allEntities.size} entries)"
        },
        tableHeaders = tableHeaders(),
        tableRowDatas = tableRowDatas(),
      )
    }

    private def tableHeaders()(implicit props: Props): Seq[VdomElement] = {
      ModelFields.allFieldsOfEntity(props.entityType).map(field => <.th(field.name): VdomElement)
    }

    private def tableRowDatas()(implicit props: Props, state: State): Seq[TableRowData] = {
      state.maybeStoreState match {
        case Some(storeState) =>
          for (entity <- storeState.allEntities) yield {
            TableRowData(
              cells = for (modelField <- ModelFields.allFieldsOfEntity(props.entityType)) yield {
                <.td(modelField.getUnsafe(entity).toString): VdomElement
              },
              deemphasize = false,
            )
          }
        case None =>
          for (_ <- 0 until 10) yield {
            TableRowData(
              cells = Seq[VdomElement](
                <.td(^.colSpan := tableHeaders().size, ^.style := js.Dictionary("color" -> "white"), "...")),
              deemphasize = false)
          }
      }
    }
  }
}
