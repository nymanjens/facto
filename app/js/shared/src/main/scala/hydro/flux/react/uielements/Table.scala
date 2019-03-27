package hydro.flux.react.uielements

import hydro.common.CollectionUtils.ifThenSeq
import hydro.common.I18n
import hydro.common.LoggingUtils.LogExceptionsCallback
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
    .initialStateFromProps(props => State(expanded = props.setExpanded.map(_.get) getOrElse true))
    .renderPS(($, props, state) =>
      <.table(
        ^^.classes(
          Seq("table", "table-bordered", "table-hover", "table-condensed", "table-overflow-elipsis") ++ props.tableClasses),
        <.thead(
          <<.ifDefined(props.title) { title =>
            <.tr(
              ^^.classes("info", "expand-on-click"),
              <.th(
                ^.colSpan := props.colSpan,
                <.span(
                  ^.className := "primary-title",
                  Bootstrap.FontAwesomeIcon(s"angle-${if (state.expanded) "down" else "right"}")(
                    ^.style := js.Dictionary("width" -> "12px")
                  ),
                  " ",
                  title
                ),
                <<.ifDefined(props.tableTitleExtra) { extra =>
                  <.span(^.className := "secondary-title", extra)
                }
              ),
              ^.onClick -->
                $.modState(_.copy(expanded = !state.expanded))
            )
          },
          <<.ifThen(state.expanded) {
            <.tr(props.tableHeaders.toTagMod)
          }
        ),
        <<.ifThen(state.expanded) {
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
    .componentWillReceiveProps($ =>
      LogExceptionsCallback {
        if ($.nextProps.setExpanded.isDefined && $.currentProps.setExpanded != $.nextProps.setExpanded) {
          $.modState(_.copy(expanded = $.nextProps.setExpanded.get.get)).runNow()
        }
    })
    .build

  // **************** API ****************//
  def apply(title: String = null,
            tableClasses: Seq[String] = Seq(),
            setExpanded: Option[Unique[Boolean]] = None,
            expandNumEntriesCallback: Option[Callback] = None,
            tableTitleExtra: VdomElement = null,
            tableHeaders: Seq[VdomElement],
            tableRowDatas: Seq[TableRowData])(implicit i18n: I18n): VdomElement = {
    component(
      Props(
        title = Option(title),
        tableClasses = tableClasses,
        setExpanded = setExpanded,
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
                           setExpanded: Option[Unique[Boolean]],
                           expandNumEntriesCallback: Option[Callback],
                           tableTitleExtra: Option[VdomElement],
                           tableHeaders: Seq[VdomElement],
                           tableRowDatas: Seq[TableRowData])(implicit val i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }
  private case class State(expanded: Boolean)
}
