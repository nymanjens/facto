package jsfacades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

object Loki {
  @JSName("loki")
  @js.native
  final class Database(dbName: String, args: js.Dictionary[js.Any] = null) extends js.Object {

    def addCollection(name: String): Collection = js.native
    def getCollection(name: String): Collection = js.native
    def getOrAddCollection(name: String): Collection = {
      val collection = getCollection(name)
      if (collection == null) {
        addCollection(name)
      } else {
        collection
      }
    }
  }

  object Database {
    def persistent(dbName: String): Database = {
      new Database(dbName, js.Dictionary("adapter" -> new IndexedAdapter(dbName)))
    }
  }

  @JSName("LokiIndexedAdapter")
  @js.native
  final class IndexedAdapter(name: String) extends js.Object

  @js.native
  trait Collection extends js.Object {

    def insert(obj: js.Dictionary[js.Any]): Unit = js.native
    def find(filter: js.Dictionary[js.Any]): js.Array[js.Dictionary[js.Any]] = js.native
  }
}
