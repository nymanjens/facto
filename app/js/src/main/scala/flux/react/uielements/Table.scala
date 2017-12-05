package flux.react.uielements

import common.I18n
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

object Table {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) =>
      <.table(
        ^^.classes(
          Seq("table", "table-bordered", "table-hover", "table-condensed", "table-overflow-elipsis") ++ props.tableClasses),
        <.thead(
          <.tr(^^.classes("info"), <.th(^.colSpan := props.colSpan, props.title)),
          <<.ifThen(props.expanded) {
            <.tr(props.tableHeaders.toTagMod)
          }
        ),
        <<.ifThen(props.expanded) {
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
    .build

  // **************** API ****************//
  def apply(title: String,
            tableClasses: Seq[String] = Seq(),
            expanded: Boolean,
            expandNumEntriesCallback: Option[Callback] = None,
            tableHeaders: Seq[VdomElement],
            tableDatas: Seq[Seq[VdomElement]])(implicit i18n: I18n): VdomElement = {
    component(Props(title, tableClasses, expanded, expandNumEntriesCallback, tableHeaders, tableDatas, i18n))
  }

  // **************** Private inner types ****************//
  private case class Props(title: String,
                           tableClasses: Seq[String],
                           expanded: Boolean,
                           expandNumEntriesCallback: Option[Callback],
                           tableHeaders: Seq[VdomElement],
                           tableDatas: Seq[Seq[VdomElement]],
                           i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }
}
