package viewhelpers.accounting

import play.twirl.api.Html
import play.api.mvc.Call

object Table {

  def apply[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = 20, colspan: Int = 9999,
               entries: Seq[T])
              (tableHeaders: Html)
              (entryToTableDatas: (T, Int) => Html): Html = {
    val tableDatas = entries.zipWithIndex.map(entryToTableDatas.tupled)
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }
}
