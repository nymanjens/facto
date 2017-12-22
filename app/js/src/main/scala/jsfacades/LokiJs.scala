package jsfacades

import common.LoggingUtils.logExceptions
import common.ScalaUtils
import jsfacades.LokiJs.Sorting.KeyWithDirection

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobal
import scala2js.Converters._
import scala2js.Scala2Js

object LokiJs {
  @JSGlobal("loki")
  @js.native
  private final class DatabaseFacade(dbName: String, args: js.Dictionary[js.Any] = null) extends js.Object {

    def addCollection(name: String): CollectionFacade = js.native
    def getCollection(name: String): CollectionFacade = js.native

    def saveDatabase(callback: js.Function0[Unit]): Unit = js.native
    def loadDatabase(properties: js.Dictionary[js.Any], callback: js.Function0[Unit]): Unit = js.native
  }

  final class Database(facade: DatabaseFacade) {
    def getOrAddCollection[E: Scala2Js.MapConverter](name: String): Collection[E] = {
      val collection = facade.getCollection(name)
      if (collection == null) {
        new Collection(facade.addCollection(name))
      } else {
        new Collection(collection)
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
                      case None => callback(null)
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
  private trait CollectionFacade extends js.Object {

    def chain(): ResultSetFacade = js.native

    def insert(obj: js.Dictionary[js.Any]): Unit = js.native
    def update(obj: js.Dictionary[js.Any]): Unit = js.native
    def findAndRemove(filter: js.Dictionary[js.Any]): Unit = js.native
    def clear(): Unit = js.native
  }

  final class Collection[E: Scala2Js.MapConverter](facade: CollectionFacade) {

    def chain(): ResultSet[E] = new ResultSet[E](facade.chain())

    def insert(obj: E): Unit = facade.insert(Scala2Js.toJsMap(obj))
    def update(obj: E): Unit = facade.update(Scala2Js.toJsMap(obj))
    def findAndRemove[V: Scala2Js.Converter](key: String, value: V): Unit =
      facade.findAndRemove(js.Dictionary(key -> Scala2Js.toJs(value)))
    def clear(): Unit = facade.clear()
  }

  @js.native
  private trait ResultSetFacade extends js.Object {

    // **************** Intermediary operations **************** //
    def find(filter: js.Dictionary[js.Any]): ResultSetFacade = js.native
    def compoundsort(properties: js.Array[js.Array[js.Any]]): ResultSetFacade = js.native
    def limit(quantity: Int): ResultSetFacade = js.native

    // **************** Terminal operations **************** //
    def data(): js.Array[js.Dictionary[js.Any]] = js.native
    def count(): Int = js.native
  }

  final class ResultSet[E: Scala2Js.MapConverter](facade: ResultSetFacade) {
    // **************** Intermediary operations **************** //
    def filter(filter: Filter): ResultSet[E] = {
      def toFilterDictionary(filter: Filter): js.Dictionary[js.Any] = filter match {
        case Filter.KeyValueFilter(operation, key, value) =>
          js.Dictionary(key -> js.Dictionary(operation.mongoModifier -> value))
        case Filter.AggregateFilter(operation, filters) =>
          js.Dictionary(operation.mongoModifier -> filters.map(toFilterDictionary).toJSArray)
      }
      val filterDictionary = toFilterDictionary(filter)
      new ResultSet[E](facade.find(filterDictionary))
    }

    def sort(sorting: LokiJs.Sorting): ResultSet[E] = {
      val properties: js.Array[js.Array[js.Any]] = {
        val result: Seq[js.Array[js.Any]] = sorting.keysWithDirection map {
          case KeyWithDirection(key, isDesc) => js.Array[js.Any](key, isDesc)
        }
        result.toJSArray
      }
      new ResultSet[E](facade.compoundsort(properties))
    }

    def limit(quantity: Int): ResultSet[E] = new ResultSet[E](facade.limit(quantity))

    // **************** Terminal operations **************** //
    def data(): Seq[E] = Scala2Js.toScala[Seq[E]](facade.data())
    def count(): Int = facade.count()
  }

  sealed trait Filter
  object Filter {
    case class KeyValueFilter(operation: Operation, key: String, value: js.Any) extends Filter
    case class AggregateFilter(operation: Operation, filters: Seq[Filter]) extends Filter

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

  case class Sorting(keysWithDirection: Seq[KeyWithDirection])
  object Sorting {
    case class KeyWithDirection(key: String, isDesc: Boolean)
  }
}
