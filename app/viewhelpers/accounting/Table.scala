package viewhelpers.accounting

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import common.cache.UniquelyHashable
import common.cache.UniquelyHashable.UniquelyHashableIterableFunnel
import common.cache.sync.SynchronizedCache
import common.cache.versioned.VersionedKeyValueCache
import org.joda.time.Duration
import play.api.i18n.Messages
import play.api.mvc.Call
import play.twirl.api.Html

object Table {

  private val defaultNumEntriesShown = 20

  private val tableCache: VersionedKeyValueCache[TableIdentifier, TableContentIdentifier, Html] =
    VersionedKeyValueCache.hashingKeys()
  private val rowCache: SynchronizedCache[RowIdentifier, Html] =
    SynchronizedCache.hashingKeys(expireAfterAccess = Duration.standardHours(32))
  private val indexedRowCache: SynchronizedCache[IndexedRowIdentifier, Html] =
    SynchronizedCache.hashingKeys(expireAfterAccess = Duration.standardHours(32))

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


  def cached[T <: UniquelyHashable](title: String,
                                    tableTypeIdentifierForCache: String,
                                    tableClasses: String = "",
                                    allEntriesLink: Option[Call] = None,
                                    numEntriesShownByDefault: Int = defaultNumEntriesShown,
                                    colspan: Int = 9999,
                                    entries: Seq[T])
                                   (tableHeaders: Html)
                                   (entryToTableDatas: T => Html)
                                   (implicit messages: Messages): Html = {
    val tableIdentifier = TableIdentifier(tableTypeIdentifierForCache, numEntriesShownByDefault)
    val tableContentIdentifier = TableContentIdentifier(entries)

    tableCache.getOrCalculate(tableIdentifier, tableContentIdentifier, () => {
      val tableDatas = entries.map { entry =>
        val rowIdentifier = RowIdentifier(tableIdentifier, entry)
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
                                                     (entryToTableDatas: (T, Int) => Html)
                                                     (implicit messages: Messages): Html = {
    val tableIdentifier = TableIdentifier(tableTypeIdentifierForCache, numEntriesShownByDefault)
    val tableContentIdentifier = TableContentIdentifier(entries)

    tableCache.getOrCalculate(tableIdentifier, tableContentIdentifier, () => {
      val tableDatas = entries.zipWithIndex.map { case (entry, index) =>
        val rowIdentifier = IndexedRowIdentifier(tableIdentifier, entry, index)
        indexedRowCache.getOrCalculate(rowIdentifier, () => entryToTableDatas(entry, index))
      }
      views.html.accounting.parts.Table(title, tableClasses, allEntriesLink, numEntriesShownByDefault, colspan, tableHeaders, tableDatas)
    })
  }

  case class TableIdentifier(tableType: String,
                             numEntriesShownByDefault: Int) extends UniquelyHashable {

    override val uniqueHash = {
      Hashing.sha1().newHasher()
        .putString(tableType, Charsets.UTF_8)
        .putInt(numEntriesShownByDefault)
        .hash()
    }
  }

  case class TableContentIdentifier(entries: Seq[UniquelyHashable]) extends UniquelyHashable {

    override val uniqueHash = {
      Hashing.sha1().newHasher()
        .putObject(entries, UniquelyHashableIterableFunnel)
        .hash()
    }
  }

  case class RowIdentifier(tableIdentifier: TableIdentifier, entry: UniquelyHashable) extends UniquelyHashable {

    override val uniqueHash = {
      Hashing.sha1().newHasher()
        .putObject(Seq(tableIdentifier, entry), UniquelyHashableIterableFunnel)
        .hash()
    }
  }
  case class IndexedRowIdentifier(tableIdentifier: TableIdentifier, entry: UniquelyHashable, index: Int) extends UniquelyHashable {

    override val uniqueHash = {
      Hashing.sha1().newHasher()
        .putObject(Seq(tableIdentifier, entry), UniquelyHashableIterableFunnel)
        .putInt(index)
        .hash()
    }
  }
}
