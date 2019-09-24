package hydro.flux.react.uielements

import hydro.common.CollectionUtils.ifThenSeq
import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

object Table extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      title: String = null,
      tableClasses: Seq[String] = Seq(),
      expanded: Boolean = true,
      onToggleCollapsedExpanded: Option[() => Unit] = None,
      expandNumEntriesCallback: Option[Callback] = None,
      tableTitleExtra: VdomElement = null,
      tableHeaders: Seq[VdomElement],
      tableRowDatas: Seq[TableRowData],
  )(implicit i18n: I18n): VdomElement = {
    component(
      Props(
        title = Option(title),
        tableClasses = tableClasses,
        expanded = expanded,
        onToggleCollapsedExpanded = onToggleCollapsedExpanded,
        expandNumEntriesCallback = expandNumEntriesCallback,
        tableTitleExtra = Option(tableTitleExtra),
        tableHeaders = tableHeaders,
        tableRowDatas = tableRowDatas
      ))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      title: Option[String],
      tableClasses: Seq[String],
      expanded: Boolean,
      onToggleCollapsedExpanded: Option[() => Unit],
      expandNumEntriesCallback: Option[Callback],
      tableTitleExtra: Option[VdomElement],
      tableHeaders: Seq[VdomElement],
      tableRowDatas: Seq[TableRowData],
  )(implicit val i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: Unit) = {
      <.table(
        ^^.classes(
          Seq("table", "table-bordered", "table-hover", "table-condensed", "table-overflow-elipsis") ++ props.tableClasses),
        <.thead(
          <<.ifDefined(props.title) { title =>
            <.tr(
              ^^.classes("info"),
              ^^.ifDefined(props.onToggleCollapsedExpanded) { _ =>
                ^^.classes("expand-on-click")
              },
              <.th(
                ^.colSpan := props.colSpan,
                <.span(
                  ^.className := "primary-title",
                  Bootstrap.FontAwesomeIcon(if (props.expanded) "angle-down" else "angle-right")(
                    ^.style := js.Dictionary("width" -> "12px")
                  ),
                  " ",
                  title
                ),
                <<.ifDefined(props.tableTitleExtra) { extra =>
                  <.span(^.className := "secondary-title", extra)
                }
              ),
              ^^.ifDefined(props.onToggleCollapsedExpanded) { onToggle =>
                ^.onClick --> Callback(onToggle())
              },
            )
          },
          <<.ifThen(props.expanded) {
            <.tr(props.tableHeaders.toTagMod)
          }
        ),
        <<.ifThen(props.expanded) {
          <.tbody(
            props.tableRowDatas.zipWithIndex.map {
              case (TableRowData(tableData, deemphasize), index) =>
                <.tr(
                  ^.key := s"row-$index",
                  ^^.classes("data-row" +: ifThenSeq(deemphasize, "deemphasized")),
                  tableData.toTagMod)
            }.toVdomArray,
            if (props.tableRowDatas.isEmpty) {
              <.tr(
                <.td(^.colSpan := props.colSpan, ^^.classes("no-entries"), props.i18n("app.no-entries"))
              )
            } else if (props.expandNumEntriesCallback.isDefined) {
              <.tr(
                <.td(
                  ^.colSpan := props.colSpan,
                  ^.style := js.Dictionary("textAlign" -> "center"),
                  Bootstrap.Button(size = Size.sm, circle = true, tag = <.a)(
                    ^.onClick --> props.expandNumEntriesCallback.get,
                    ^.className := "expand-num-entries",
                    Bootstrap.FontAwesomeIcon("ellipsis-h"),
                  )
                )
              )
            } else {
              EmptyVdom
            }
          )
        }
      )
    }
  }

  // **************** Public inner types ****************//
  case class TableRowData(cells: Seq[VdomElement], deemphasize: Boolean = false)
}
