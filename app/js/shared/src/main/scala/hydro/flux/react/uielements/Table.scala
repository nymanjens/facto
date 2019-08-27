package hydro.flux.react.uielements

import hydro.common.CollectionUtils.ifThenSeq
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.Unique
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Size
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

object Table {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(($, props) =>
      <.table(
        ^^.classes(
          Seq("table", "table-bordered", "table-hover", "table-condensed", "table-overflow-elipsis") ++ props.tableClasses),
        <.thead(
          <<.ifDefined(props.title) { title =>
            <.tr(
              ^^.classes("info"),
              ^^.ifDefined(props.toggleCollapsedExpandedCallback) { callback =>
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
              ^^.ifDefined(props.toggleCollapsedExpandedCallback) { callback =>
                ^.onClick --> callback
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
    ))
    .build

  // **************** API ****************//
  def apply(title: String = null,
            tableClasses: Seq[String] = Seq(),
            expanded: Boolean = true,
            toggleCollapsedExpandedCallback: Option[Callback] = None,
            expandNumEntriesCallback: Option[Callback] = None,
            tableTitleExtra: VdomElement = null,
            tableHeaders: Seq[VdomElement],
            tableRowDatas: Seq[TableRowData])(implicit i18n: I18n): VdomElement = {
    component(
      Props(
        title = Option(title),
        tableClasses = tableClasses,
        expanded = expanded,
        toggleCollapsedExpandedCallback = toggleCollapsedExpandedCallback,
        expandNumEntriesCallback = expandNumEntriesCallback,
        tableTitleExtra = Option(tableTitleExtra),
        tableHeaders = tableHeaders,
        tableRowDatas = tableRowDatas
      ))
  }

  // **************** Public inner types ****************//
  case class TableRowData(cells: Seq[VdomElement], deemphasize: Boolean = false)

  // **************** Private inner types ****************//
  private case class Props(title: Option[String],
                           tableClasses: Seq[String],
                           expanded: Boolean,
                           toggleCollapsedExpandedCallback: Option[Callback],
                           expandNumEntriesCallback: Option[Callback],
                           tableTitleExtra: Option[VdomElement],
                           tableHeaders: Seq[VdomElement],
                           tableRowDatas: Seq[TableRowData])(implicit val i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }
}
