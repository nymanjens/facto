package jsfacades

import common.LoggingUtils.logExceptions
import common.ScalaUtils

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobal

object LokiJs {
  @JSGlobal("loki")
  @js.native
  private final class DatabaseFacade(dbName: String, args: js.Dictionary[js.Any] = null) extends js.Object {

    def addCollection(name: String): Collection = js.native
    def getCollection(name: String): Collection = js.native

    def saveDatabase(callback: js.Function0[Unit]): Unit = js.native
    def loadDatabase(properties: js.Dictionary[js.Any], callback: js.Function0[Unit]): Unit = js.native
  }

  final class Database(facade: DatabaseFacade) {
    def getOrAddCollection(name: String): Collection = {
      val collection = facade.getCollection(name)
      if (collection == null) {
        facade.addCollection(name)
      } else {
        collection
      }
    }

    def saveDatabase(): Future[Unit] = {
      val (callback, future) = ScalaUtils.callbackSettingFuturePair()
      facade.saveDatabase(callback)
      future
    }

    def loadDatabase(): Future[Unit] = {
      val (callback, future) = ScalaUtils.callbackSettingFuturePair()
      facade.loadDatabase(properties = js.Dictionary(), callback)
      future
    }
  }

  object Database {
    def persistent(dbName: String,
                   persistedStringCodex: PersistedStringCodex = PersistedStringCodex.NullCodex): Database = {
      new Database(
        new DatabaseFacade(
          dbName,
          js.Dictionary("adapter" -> Adapter
            .toAdapterDecorator(persistedStringCodex, new Adapter.IndexedAdapter(dbName)))))
    }

    def inMemoryForTests(
        dbName: String,
        persistedStringCodex: PersistedStringCodex = PersistedStringCodex.NullCodex): Database = {
      new Database(
        new DatabaseFacade(
          dbName,
          js.Dictionary(
            "adapter" -> Adapter.toAdapterDecorator(persistedStringCodex, new Adapter.MemoryAdapter()),
            "env" -> "BROWSER")))
    }
  }

  trait PersistedStringCodex {
    def encodeBeforeSave(dbString: String): String
    def decodeAfterLoad(encodedString: String): Option[String]
  }
  object PersistedStringCodex {
    object NullCodex extends PersistedStringCodex {
      override def encodeBeforeSave(dbString: String) = dbString
      override def decodeAfterLoad(encodedString: String) = Some(encodedString)
    }
  }

  @js.native
  private trait Adapter extends js.Object {
    def saveDatabase(dbName: String, dbString: String, callback: js.Function0[Unit]): Unit = js.native
    def loadDatabase(dbName: String, callback: js.Function1[js.Any, Unit]): Unit = js.native
  }
  private object Adapter {
    @JSGlobal("LokiIndexedAdapter")
    @js.native
    final class IndexedAdapter(name: String) extends Adapter

    @JSGlobal("loki.LokiMemoryAdapter")
    @js.native
    final class MemoryAdapter() extends Adapter

    def toAdapterDecorator(codex: PersistedStringCodex, delegate: Adapter): Adapter =
      js.Dynamic
        .literal(
          saveDatabase = (dbName: String, dbString: String, callback: js.Function0[Unit]) =>
            logExceptions {
              delegate.saveDatabase(dbName, codex.encodeBeforeSave(dbString), callback)
          },
          loadDatabase = (dbName: String, callback: js.Function1[js.Any, Unit]) =>
            logExceptions {
              delegate.loadDatabase(
                dbName,
                callback = {
                  case result @ null =>
                    callback(result)
                  case result: AnyRef if result.getClass == classOf[String] =>
                    val encodedDbString = result.asInstanceOf[String]

                    codex.decodeAfterLoad(encodedDbString) match {
                      case Some(dbString) => callback(dbString)
                      case None           => callback(null)
                    }
                  case result =>
                    callback(result)
                }
              )
          }
        )
        .asInstanceOf[Adapter]
  }

  @js.native
  trait Collection extends js.Object {

    def chain(): ResultSet = js.native

    def insert(obj: js.Dictionary[js.Any]): Unit = js.native
    def update(obj: js.Dictionary[js.Any]): Unit = js.native
    // Use FilterFactory to create filters
    def findAndRemove(filter: js.Dictionary[js.Any]): Unit = js.native
    def clear(): Unit = js.native
  }

  @js.native
  trait ResultSet extends js.Object {
    // **************** Intermediary operations **************** //
    // Use FilterFactory to create filters
    def find(filter: js.Dictionary[js.Any]): ResultSet = js.native
    // Use SortingFactory to create properties
    def compoundsort(properties: js.Array[js.Array[js.Any]]): ResultSet = js.native
    def limit(quantity: Int): ResultSet = js.native

    // **************** Terminal operations **************** //
    def data(): js.Array[js.Dictionary[js.Any]] = js.native
    def count(): Int = js.native
  }

  object FilterFactory {
    def keyValueFilter(operation: Operation, key: String, value: js.Any): js.Dictionary[js.Any] =
      js.Dictionary(key -> js.Dictionary(operation.mongoModifier -> value))

    def aggregateFilter(operation: Operation, filters: Seq[js.Dictionary[js.Any]]): js.Dictionary[js.Any] =
      js.Dictionary(operation.mongoModifier -> filters.toJSArray)

    sealed abstract class Operation(val mongoModifier: String)
    object Operation {
      object Equal extends Operation("$eq")
      object NotEqual extends Operation("$ne")
      object GreaterThan extends Operation("$gt")
      object GreaterOrEqualThan extends Operation("$gte")
      object LessThan extends Operation("$lt")
      object AnyOf extends Operation("$in")
      object NoneOf extends Operation("$nin")
      object Regex extends Operation("$regex")
      object Contains extends Operation("$contains")
      object ContainsNone extends Operation("$containsNone")
      object Or extends Operation("$or")
      object And extends Operation("$and")
    }
  }

  object SortingFactory {
    def keysWithDirection(keyWithDirection: Seq[KeyWithDirection]): js.Array[js.Array[js.Any]] = {
      val result: Seq[js.Array[js.Any]] = keyWithDirection map {
        case KeyWithDirection(key, isDesc) => js.Array[js.Any](key, isDesc)
      }
      result.toJSArray
    }
    case class KeyWithDirection(key: String, isDesc: Boolean)
  }
}
