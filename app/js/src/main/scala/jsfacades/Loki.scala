package jsfacades

import common.ScalaUtils

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{Any, Array, Dictionary}
import scala.scalajs.js.annotation.JSName

object Loki {
  @JSName("loki")
  @js.native
  private final class DatabaseFacade(dbName: String, args: js.Dictionary[js.Any] = null) extends js.Object {

    def addCollection(name: String): CollectionFacade = js.native
    def getCollection(name: String): CollectionFacade = js.native

    def saveDatabase(callback: js.Function0[Unit]): Unit = js.native
    def loadDatabase(properties: js.Dictionary[js.Any], callback: js.Function0[Unit]): Unit = js.native
  }

  final class Database(facade: DatabaseFacade) {
    def getOrAddCollection(name: String): Collection = {
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
    def persistent(dbName: String): Database = {
      new Database(
        new DatabaseFacade(
          dbName, js.Dictionary("adapter" -> new IndexedAdapter(dbName))))
    }
  }

  @JSName("LokiIndexedAdapter")
  @js.native
  final class IndexedAdapter(name: String) extends js.Object

  @js.native
  private trait CollectionFacade extends js.Object {

    def chain(): ResultSetFacade = js.native
    def find(filter: js.Dictionary[js.Any]): js.Array[js.Dictionary[js.Any]] = js.native
    def findOne(filter: js.Dictionary[js.Any]): /* nullable */ js.Dictionary[js.Any] = js.native

    def insert(obj: js.Dictionary[js.Any]): Unit = js.native
    def findAndRemove(filter: js.Dictionary[js.Any]): Unit = js.native
    def clear(): Unit = js.native
  }

  final class Collection(facade: CollectionFacade) {

    def chain(): ResultSet = new ResultSet(facade.chain())
    def find(filter: (String, js.Any)*): js.Array[js.Dictionary[js.Any]] = facade.find(js.Dictionary(filter: _*))
    def findOne(filter: (String, js.Any)*): Option[js.Dictionary[js.Any]] = Option(facade.findOne(js.Dictionary(filter: _*)))

    def insert(obj: js.Dictionary[js.Any]): Unit = facade.insert(obj)
    def findAndRemove(filter: (String, js.Any)*): Unit = facade.findAndRemove(js.Dictionary(filter: _*))
    def clear(): Unit = facade.clear()
  }

  @js.native
  private trait ResultSetFacade extends js.Object {

    def find(filter: js.Dictionary[js.Any]): ResultSetFacade = js.native
    def data(): js.Array[js.Dictionary[js.Any]] = js.native
    def count(): Int = js.native
  }

  final class ResultSet(facade: ResultSetFacade) {

    def find(filter: (String, js.Any)*): ResultSet = new ResultSet(facade.find(js.Dictionary(filter: _*)))
    def data(): js.Array[js.Dictionary[js.Any]] = facade.data()
    def count(): Int = facade.count()
  }
}
