package viewhelpers.accounting

import play.twirl.api.Html
import play.api.mvc.Call

import org.joda.time.Duration

import common.cache.{SynchronizedCache, CacheMaintenanceManager}
import models.manager.Entity


object Table {

  private val defaultNumEntriesShown = 20

  //  private val tableCache: SynchronizedCache[TableCacheIdentifier, Html] =
  //    SynchronizedCache(expireAfterAccess = Duration.standardHours(32))
  private val rowCache: SynchronizedCache[RowCacheIdentifier, Html] =
    SynchronizedCache(expireAfterAccess = Duration.standardHours(32))

  def apply[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = defaultNumEntriesShown, colspan: Int = 9999,
               entries: Seq[T])
              (tableHeaders: Html)
              (entryToTableDatas: T => Html): Html = {
    val tableDatas = entries map entryToTableDatas
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }

  def withIndexedEntries[T](title: String, tableClasses: String = "", allEntriesLink: Option[Call] = None, numEntriesShownByDefault: Int = defaultNumEntriesShown, colspan: Int = 9999,
                            entries: Seq[T])
                           (tableHeaders: Html)
                           (entryToTableDatas: (T, Int) => Html): Html = {
    val tableDatas = entries.zipWithIndex.map(entryToTableDatas.tupled)
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
  }


  def cached[T](title: String,
                tableTypeIdentifierForCache: AnyRef,
                tableClasses: String = "",
                allEntriesLink: Option[Call] = None,
                numEntriesShownByDefault: Int = defaultNumEntriesShown,
                colspan: Int = 9999,
                entries: Seq[T])
               (tableHeaders: Html)
               (entryToTableDatas: T => Html): Html = {
    val tableIdentifier = TableCacheIdentifier(tableTypeIdentifierForCache, entries, numEntriesShownByDefault)

    //    tableCache.getOrCalculate(tableIdentifier, () => {
    val tableDatas = entries.map { entry =>
      val rowIdentifier = RowCacheIdentifier(tableIdentifier, entry)
      rowCache.getOrCalculate(rowIdentifier, () => entryToTableDatas(entry))
    }
    views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
    //    })
  }

  case class TableCacheIdentifier(tableType: AnyRef, entries: Seq[_], numEntriesShownByDefault: Int)
  case class RowCacheIdentifier(tableIdentifier: TableCacheIdentifier, entry: Any)
}
