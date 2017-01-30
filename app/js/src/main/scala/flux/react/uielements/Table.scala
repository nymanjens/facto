package flux.react.uielements

import scala.scalajs.js
import common.I18n
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^

import scala.collection.immutable.Seq

object Table {
  private case class Props(title: String,
                           tableClasses: Seq[String],
                           moreEntriesCallback: Option[Callback],
                           tableHeaders: Seq[ReactElement],
                           tableDatas: Seq[Seq[ReactElement]],
                           i18n: I18n) {
    def colSpan: Int = tableHeaders.size
  }
  private val component = ReactComponentB[Props]("Table")
    .renderP((_, props) =>
      <.table(^^.classes(Seq("table", "table-bordered", "table-hover", "table-condensed", "table-overflow-elipsis", "add-toc-level-2") ++ props.tableClasses),
        <.thead(
          <.tr(^^.classes("info"),
            <.th(^^.classes("toc-title"),
              ^.colSpan := props.colSpan,
              props.title
            )
          ),
          <.tr(props.tableHeaders)
        ),
        <.tbody(
          props.tableDatas.map(tableData => <.tr(tableData)),
          if (props.tableDatas.isEmpty) {
            <.tr(
              <.td(^.colSpan := props.colSpan, ^^.classes("no-entries"),
                props.i18n("facto.no-entries")
              )
            )
          } else if (props.moreEntriesCallback.isDefined) {
            <.tr(
              <.td(^.colSpan := props.colSpan,
                ^.style := js.Dictionary("textAlign" -> "center"),
                <.a(^.onClick --> props.moreEntriesCallback.get,
                  ^.tpe := "button",
                  ^^.classes("btn", "btn-sm", "btn-default", "btn-circle", "@btnClasses"
                  ),
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
            moreEntriesCallback: Option[Callback] = None,
            tableHeaders: Seq[ReactElement],
            tableDatas: Seq[Seq[ReactElement]])(implicit i18n: I18n): ReactElement = {
    component(Props(title, tableClasses, moreEntriesCallback, tableHeaders, tableDatas, i18n))
  }
}
