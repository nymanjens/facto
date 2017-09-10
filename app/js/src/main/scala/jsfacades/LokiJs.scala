package jsfacades

import common.LoggingUtils.logExceptions
import common.GuavaReplacement.Iterables.getOnlyElement
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
                callback = result => {
                  result match {
                    case null =>
                      callback(result)
                    case _ if result.getClass == classOf[String] =>
                      val encodedDbString = result.asInstanceOf[String]

                      codex.decodeAfterLoad(encodedDbString) match {
                        case Some(dbString) => callback(dbString)
                        case None => callback(null)
                      }
                    case _ =>
                      callback(result)
                  }
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
    def findAndRemove(filter: js.Dictionary[js.Any]): Unit = js.native
    def clear(): Unit = js.native
  }

  final class Collection[E: Scala2Js.MapConverter](facade: CollectionFacade) {

    def chain(): ResultSet[E] = new ResultSet.Impl[E](facade.chain())

    def insert(obj: E): Unit = facade.insert(Scala2Js.toJsMap(obj))
    def findAndRemove[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): Unit =
      facade.findAndRemove(js.Dictionary(Scala2Js.Key.toJsPair(key -> value)))
    def clear(): Unit = facade.clear()
  }

  @js.native
  private trait ResultSetFacade extends js.Object {

    // **************** Intermediary operations **************** //
    def find(filter: js.Dictionary[js.Any], firstOnly: Boolean = false): ResultSetFacade = js.native
    def compoundsort(properties: js.Array[js.Array[js.Any]]): ResultSetFacade = js.native
    def limit(quantity: Int): ResultSetFacade = js.native

    // **************** Terminal operations **************** //
    def data(): js.Array[js.Dictionary[js.Any]] = js.native
    def count(): Int = js.native
  }

  trait ResultSet[E] {
    // **************** Intermediary operations **************** //
    def filter[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E]
    def filterNot[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E]
    def filterGreaterThan[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E]
    def filterLessThan[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E]

    def sort(sorting: LokiJs.Sorting[E]): ResultSet[E]
    def limit(quantity: Int): ResultSet[E]

    // **************** Terminal operations **************** //
    def findOne[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): Option[E]
    def data(): Seq[E]
    def count(): Int
  }

  case class Sorting[E] private (private[LokiJs] val keysWithDirection: Seq[KeyWithDirection[E]]) {
    def thenAscBy[V: Ordering](key: Scala2Js.Key[V, E]): Sorting[E] = thenBy(key, isDesc = false)
    def thenDescBy[V: Ordering](key: Scala2Js.Key[V, E]): Sorting[E] = thenBy(key, isDesc = true)
    def thenBy[V: Ordering](key: Scala2Js.Key[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(keysWithDirection :+ KeyWithDirection(key, isDesc = isDesc))
  }
  object Sorting {
    def ascBy[V: Ordering, E](key: Scala2Js.Key[V, E]): Sorting[E] = by(key, isDesc = false)
    def descBy[V: Ordering, E](key: Scala2Js.Key[V, E]): Sorting[E] = by(key, isDesc = true)
    def by[V: Ordering, E](key: Scala2Js.Key[V, E], isDesc: Boolean): Sorting[E] =
      Sorting(Seq(KeyWithDirection(key, isDesc = isDesc)))

    private[LokiJs] case class KeyWithDirection[E](key: Scala2Js.Key[_, E], isDesc: Boolean)
  }

  object ResultSet {

    def empty[E: Scala2Js.MapConverter]: ResultSet[E] = new ResultSet.Fake(Seq())

    private[jsfacades] final class Impl[E: Scala2Js.MapConverter](facade: ResultSetFacade)
        extends ResultSet[E] {

      // **************** Intermediary operations **************** //
      override def filter[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V) = {
        new ResultSet.Impl[E](facade.find(js.Dictionary(Scala2Js.Key.toJsPair(key -> value))))
      }

      override def filterNot[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E] =
        filterWithModifier("$ne", key, value)

      override def filterGreaterThan[V: Scala2Js.Converter](key: Scala2Js.Key[V, E],
                                                            value: V): ResultSet[E] =
        filterWithModifier("$gt", key, value)
      override def filterLessThan[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E] =
        filterWithModifier("$lt", key, value)
      private def filterWithModifier[V: Scala2Js.Converter](modifier: String,
                                                            key: Scala2Js.Key[V, E],
                                                            value: V): ResultSet[E] = {
        val pair = Scala2Js.Key.toJsPair(key -> value)
        new ResultSet.Impl[E](facade.find(js.Dictionary(pair._1 -> js.Dictionary(modifier -> pair._2))))
      }

      override def sort(sorting: LokiJs.Sorting[E]) = {
        val properties: js.Array[js.Array[js.Any]] = {
          val result: Seq[js.Array[js.Any]] = sorting.keysWithDirection map
            (keyWithDirection => js.Array[js.Any](keyWithDirection.key.name, keyWithDirection.isDesc))
          result.toJSArray
        }
        new ResultSet.Impl[E](facade.compoundsort(properties))
      }

      override def limit(quantity: Int) = {
        new ResultSet.Impl[E](facade.limit(quantity))
      }

      // **************** Terminal operations **************** //
      override def findOne[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V) = {
        val data = facade.find(js.Dictionary(Scala2Js.Key.toJsPair(key -> value)), firstOnly = true).data()
        if (data.length >= 1) {
          Option(Scala2Js.toScala[E](getOnlyElement(data)))
        } else {
          None
        }
      }

      override def data() = {
        Scala2Js.toScala[Seq[E]](facade.data())
      }

      override def count() = {
        facade.count()
      }
    }

    final class Fake[E: Scala2Js.MapConverter](entities: Seq[E]) extends ResultSet[E] {

      implicit private val jsValueOrdering: Ordering[js.Any] = (x, y) => {
        if (x.getClass == classOf[String]) {
          x.asInstanceOf[String] compareTo y.toString
        } else if (x.isInstanceOf[Int]) {
          x.asInstanceOf[Int] compareTo y.asInstanceOf[Int]
        } else {
          ???
        }
      }

      // **************** Intermediary operations **************** //
      override def filter[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V) =
        new ResultSet.Fake(entities.filter { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          jsMap(key.name) == Scala2Js.toJs(value)
        })
      override def filterNot[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V) =
        new ResultSet.Fake(entities.filterNot { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          jsMap(key.name) == Scala2Js.toJs(value)
        })

      override def filterGreaterThan[V: Scala2Js.Converter](key: Scala2Js.Key[V, E],
                                                            value: V): ResultSet[E] =
        new ResultSet.Fake(entities.filter { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          jsValueOrdering.gt(jsMap(key.name), Scala2Js.toJs(value))
        })

      override def filterLessThan[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E] =
        new ResultSet.Fake(entities.filter { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          jsValueOrdering.lt(jsMap(key.name), Scala2Js.toJs(value))
        })

      override def sort(sorting: LokiJs.Sorting[E]) = {
        val newData: Seq[E] = {
          entities.sortWith(lt = (lhs, rhs) => {
            val lhsMap = Scala2Js.toJsMap(lhs)
            val rhsMap = Scala2Js.toJsMap(rhs)
            val results = for {
              KeyWithDirection(key, isDesc) <- sorting.keysWithDirection
              if !jsValueOrdering.equiv(lhsMap(key.name), rhsMap(key.name))
            } yield {
              if (jsValueOrdering.lt(lhsMap(key.name), rhsMap(key.name))) {
                !isDesc
              } else {
                isDesc
              }
            }
            results.headOption getOrElse false
          })
        }
        new ResultSet.Fake(newData)
      }

      override def limit(quantity: Int) = new ResultSet.Fake(
        entities.take(quantity)
      )

      // **************** Terminal operations **************** //
      override def data() = entities

      override def findOne[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V) =
        filter(key, value).limit(1).data() match {
          case Seq(e) => Some(e)
          case Seq() => None
        }

      override def count() = entities.length
    }
  }
}
