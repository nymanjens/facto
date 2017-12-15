package flux.react.uielements

import common.LoggingUtils.LogExceptionsCallback
import common.{I18n, Unique}
import flux.react.ReactVdomUtils.{<<, ^^}
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
          <.tr(
            ^^.classes("info", "expand-on-click"),
            <.th(
              ^.colSpan := props.colSpan,
              <.span(
                ^.className := "primary-title",
                <.i(
                  ^.className := s"fa fa-angle-${if (state.expanded) "down" else "right"}",
                  ^.style := js.Dictionary("width" -> "12px")),
                " ",
                props.title
              ),
              <<.ifThen(props.tableTitleExtra) { extra =>
                <.span(^.className := "secondary-title", extra)
              }
            ),
            ^.onClick -->
              $.modState(_.copy(expanded = !state.expanded))
          ),
          <<.ifThen(state.expanded) {
            <.tr(props.tableHeaders.toTagMod)
          }
        ),
        <<.ifThen(state.expanded) {
          <.tbody(
            props.tableDatas.zipWithIndex.map {
              case (tableData, index) =>
                <.tr(^.key := s"row-$index", ^.className := "data-row", tableData.toTagMod)
            }.toVdomArray,
            if (props.tableDatas.isEmpty) {
              <.tr(
                <.td(^.colSpan := props.colSpan, ^^.classes("no-entries"), props.i18n("facto.no-entries"))
              )
            } else if (props.expandNumEntriesCallback.isDefined) {
              <.tr(
                <.td(
                  ^.colSpan := props.colSpan,
                  ^.style := js.Dictionary("textAlign" -> "center"),
                  <.a(
                    ^.onClick --> props.expandNumEntriesCallback.get,
                    ^.tpe := "button",
                    ^^.classes("btn", "btn-sm", "btn-default", "btn-circle", "expand-num-entries"),
                    <.i(^^.classes("fa", "fa-ellipsis-h"))
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
  def apply(title: String,
            tableClasses: Seq[String] = Seq(),
            setExpanded: Option[Unique[Boolean]] = None,
            expandNumEntriesCallback: Option[Callback] = None,
            tableTitleExtra: VdomElement = null,
            tableHeaders: Seq[VdomElement],
            tableDatas: Seq[Seq[VdomElement]])(implicit i18n: I18n): VdomElement = {
    component(
      Props(
        title = title,
        tableClasses = tableClasses,
        setExpanded = setExpanded,
        expandNumEntriesCallback = expandNumEntriesCallback,
        tableTitleExtra = Option(tableTitleExtra),
        tableHeaders = tableHeaders,
        tableDatas = tableDatas
      ))
  }

  // **************** Private inner types ****************//
  private case class Props(title: String,
                           tableClasses: Seq[String],
                           setExpanded: Option[Unique[Boolean]],
                           expandNumEntriesCallback: Option[Callback],
                           tableTitleExtra: Option[VdomElement],
                           tableHeaders: Seq[VdomElement],
                           tableDatas: Seq[Seq[VdomElement]])(implicit val i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }
  private case class State(expanded: Boolean)
}
