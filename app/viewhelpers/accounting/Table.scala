package viewhelpers.accounting

import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.Html

object Table {

  private val defaultNumEntriesShown = 20

  def apply[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = defaultNumEntriesShown, colspan: Int = 9999,
               entries: Seq[T])
              (tableHeaders: Html)
              (entryToTableDatas: T => Html)
              (implicit messages: Messages): Html = {
    val tableDatas = entries map entryToTableDatas
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }

  def withIndexedEntries[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = defaultNumEntriesShown, colspan: Int = 9999,
                            entries: Seq[T])
                           (tableHeaders: Html)
                           (entryToTableDatas: (T, Int) => Html)
                           (implicit messages: Messages): Html = {
    val tableDatas = entries.zipWithIndex.map(entryToTableDatas.tupled)
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }
}
