package viewhelpers.accounting

import com.google.common.base.Charsets
import com.google.common.hash.{Hashing}
import play.twirl.api.Html
import play.api.mvc.Call
import org.joda.time.Duration

import common.cache.{CacheMaintenanceManager, SynchronizedCache, UniquelyHashable}

object Table {

  private val defaultNumEntriesShown = 20

  private val tableCache: SynchronizedCache[TableCacheIdentifier, Html] =
    SynchronizedCache.hashingKeys(expireAfterAccess = Duration.standardHours(32))
  private val rowCache: SynchronizedCache[RowCacheIdentifier, Html] =
    SynchronizedCache.hashingKeys(expireAfterAccess = Duration.standardHours(32))
  private val indexedRowCache: SynchronizedCache[IndexedRowCacheIdentifier, Html] =
    SynchronizedCache.hashingKeys(expireAfterAccess = Duration.standardHours(32))

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


  def cached[T <: UniquelyHashable](title: String,
                                    tableTypeIdentifierForCache: String,
                                    tableClasses: String = "",
                                    allEntriesLink: Option[Call] = None,
                                    numEntriesShownByDefault: Int = defaultNumEntriesShown,
                                    colspan: Int = 9999,
                                    entries: Seq[T])
                                   (tableHeaders: Html)
                                   (entryToTableDatas: T => Html): Html = {
    val tableIdentifier = TableCacheIdentifier(tableTypeIdentifierForCache, entries, numEntriesShownByDefault)

    tableCache.getOrCalculate(tableIdentifier, () => {
      val tableDatas = entries.map { entry =>
        val rowIdentifier = RowCacheIdentifier(tableIdentifier, entry)
        rowCache.getOrCalculate(rowIdentifier, () => entryToTableDatas(entry))
      }
      views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
    })
  }


  def cachedWithIndexedEntries[T <: UniquelyHashable](title: String,
                                                      tableTypeIdentifierForCache: String,
                                                      tableClasses: String = "",
                                                      allEntriesLink: Option[Call] = None,
                                                      numEntriesShownByDefault: Int = defaultNumEntriesShown,
                                                      colspan: Int = 9999,
                                                      entries: Seq[T])
                                                     (tableHeaders: Html)
                                                     (entryToTableDatas: (T, Int) => Html): Html = {
    val tableIdentifier = TableCacheIdentifier(tableTypeIdentifierForCache, entries, numEntriesShownByDefault)

    tableCache.getOrCalculate(tableIdentifier, () => {
      val tableDatas = entries.zipWithIndex.map { case (entry, index) =>
        val rowIdentifier = IndexedRowCacheIdentifier(tableIdentifier, entry, index)
        indexedRowCache.getOrCalculate(rowIdentifier, () => entryToTableDatas(entry, index))
      }
      views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
    })
  }

  case class TableCacheIdentifier(tableType: String,
                                  entries: Seq[UniquelyHashable],
                                  numEntriesShownByDefault: Int) extends UniquelyHashable {

    override val uniqueHash = {
      val hasher = Hashing.sha1().newHasher()
      hasher.putString(tableType, Charsets.UTF_8)
      for (entry <- entries) {
        hasher.putUnencodedChars(entry.uniqueHash)
      }
      hasher.putInt(numEntriesShownByDefault)
      hasher.hash().toString
    }
  }

  case class RowCacheIdentifier(tableIdentifier: TableCacheIdentifier, entry: UniquelyHashable) extends UniquelyHashable {
    override val uniqueHash = s"$tableIdentifier!!${entry.uniqueHash}"
  }
  case class IndexedRowCacheIdentifier(tableIdentifier: TableCacheIdentifier, entry: UniquelyHashable, index: Int) extends UniquelyHashable {
    override val uniqueHash = s"$tableIdentifier!!${entry.uniqueHash}!!$index"
  }
}
