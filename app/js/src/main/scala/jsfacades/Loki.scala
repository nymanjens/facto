package jsfacades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

object Loki {
  @JSName("loki")
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

    def saveDatabase(callback: () => Unit = () => {}): Unit = facade.saveDatabase(callback)

    def loadDatabase(callback: () => Unit = () => {}): Unit = {
      facade.loadDatabase(properties = js.Dictionary(), callback)
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
  trait Collection extends js.Object {

    def insert(obj: js.Any): Unit = js.native
    def find(filter: js.Dictionary[js.Any]): js.Array[js.Dictionary[js.Any]] = js.native
  }
}
