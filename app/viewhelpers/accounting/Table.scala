package viewhelpers.accounting

import play.twirl.api.Html
import play.api.mvc.Call

import org.joda.time.Period

import common.cache.{SynchronizedCache, CacheMaintenanceManager}
import models.manager.Entity


object Table {

  def apply[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = 20, colspan: Int = 9999,
               entries: Seq[T])
              (tableHeaders: Html)
              (entryToTableDatas: T => Html): Html = {
    val tableDatas = entries map entryToTableDatas
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }

  def withIndexedEntries[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = 20, colspan: Int = 9999,
                            entries: Seq[T])
                           (tableHeaders: Html)
                           (entryToTableDatas: (T, Int) => Html): Html = {
    val tableDatas = entries.zipWithIndex.map(entryToTableDatas.tupled)
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }
}
