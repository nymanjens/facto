package flux.react.uielements

import scala.scalajs.js
import common.I18n
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^

import scala.collection.immutable.Seq

object Table {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderP((_, props) =>
      <.table(^^.classes(Seq("table", "table-bordered", "table-hover", "table-condensed", "table-overflow-elipsis") ++ props.tableClasses),
        <.thead(
          <.tr(^^.classes("info"),
            <.th(^.colSpan := props.colSpan,
              props.title
            )
          ),
          <.tr(props.tableHeaders)
        ),
        <.tbody(
          props.tableDatas.map(tableData => <.tr(
            ^.className := "data-row",
            tableData)
          ),
          if (props.tableDatas.isEmpty) {
            <.tr(
              <.td(^.colSpan := props.colSpan, ^^.classes("no-entries"),
                props.i18n("facto.no-entries")
              )
            )
          } else if (props.expandNumEntriesCallback.isDefined) {
            <.tr(
              <.td(^.colSpan := props.colSpan,
                ^.style := js.Dictionary("textAlign" -> "center"),
                <.a(^.onClick --> props.expandNumEntriesCallback.get,
                  ^.tpe := "button",
                  ^^.classes("btn", "btn-sm", "btn-default", "btn-circle", "expand-num-entries"),
                  <.i(^^.classes("fa", "fa-ellipsis-h"))
                )
              )
            )
          } else {
            Seq()
          }
        )
      )
    ).build

  def apply(title: String,
            tableClasses: Seq[String] = Seq(),
            expandNumEntriesCallback: Option[Callback] = None,
            tableHeaders: Seq[ReactElement],
            tableDatas: Seq[Seq[ReactElement]])(implicit i18n: I18n): ReactElement = {
    component(Props(title, tableClasses, expandNumEntriesCallback, tableHeaders, tableDatas, i18n))
  }

  private case class Props(title: String,
                           tableClasses: Seq[String],
                           expandNumEntriesCallback: Option[Callback],
                           tableHeaders: Seq[ReactElement],
                           tableDatas: Seq[Seq[ReactElement]],
                           i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }
}
