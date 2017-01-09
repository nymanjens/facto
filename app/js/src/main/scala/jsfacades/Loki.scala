package jsfacades

import common.GuavaReplacement.Iterables.getOnlyElement
import common.ScalaUtils

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Any
import scala.scalajs.js.annotation.JSName
import scala2js.Scala2Js
import scala2js.Converters._

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
    def persistent(dbName: String): Database = {
      new Database(
        new DatabaseFacade(
          dbName, js.Dictionary("adapter" -> new IndexedAdapter(dbName))))
    }

    def inMemoryForTests(dbName: String): Database = {
      new Database(
        new DatabaseFacade(
          dbName, js.Dictionary(
            "adapter" -> new MemoryAdapter(),
            "env" -> "BROWSER")))
    }
  }

  @JSName("LokiIndexedAdapter")
  @js.native
  final class IndexedAdapter(name: String) extends js.Object

  @JSName("loki.LokiMemoryAdapter")
  @js.native
  final class MemoryAdapter() extends js.Object

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
    def findAndRemove(filter: (String, js.Any)*): Unit = facade.findAndRemove(js.Dictionary(filter: _*))
    def clear(): Unit = facade.clear()
  }

  @js.native
  private trait ResultSetFacade extends js.Object {

    // **************** Intermediary operations **************** //
    def find(filter: js.Dictionary[js.Any], firstOnly: Boolean = false): ResultSetFacade = js.native
    def simplesort(propName: String, isDesc: Boolean = false): ResultSetFacade = js.native
    def limit(quantity: Int): ResultSetFacade = js.native

    // **************** Terminal operations **************** //
    def data(): js.Array[js.Dictionary[js.Any]] = js.native
    def count(): Int = js.native
  }

  trait ResultSet[E] {
    // **************** Intermediary operations **************** //
    def find(filter: (String, js.Any)*): ResultSet[E]
    /**
      * Loose evaluation for user to sort based on a property name. (chainable). Sorting based on the same lt/gt helper
      * functions used for binary indices.
      */
    def sort(propName: String, isDesc: Boolean = false): ResultSet[E]
    def limit(quantity: Int): ResultSet[E]

    // **************** Terminal operations **************** //
    def findOne(filter: (String, js.Any)*): Option[E]
    def data(): Seq[E]
    def count(): Int
  }

  object ResultSet {
    def empty[E: Scala2Js.MapConverter]: ResultSet[E] = new ResultSet.Fake(Seq())

    private[jsfacades] final class Impl[E: Scala2Js.MapConverter](facade: ResultSetFacade) extends ResultSet[E] {

      // **************** Intermediary operations **************** //
      override def find(filter: (String, js.Any)*) = {
        new ResultSet.Impl[E](facade.find(js.Dictionary(filter: _*)))
      }

      override def sort(propName: String, isDesc: Boolean) = {
        new ResultSet.Impl[E](facade.simplesort(propName, isDesc))
      }

      override def limit(quantity: Int) = {
        new ResultSet.Impl[E](facade.limit(quantity))
      }

      // **************** Terminal operations **************** //
      override def findOne(filter: (String, js.Any)*) = {
        val data = facade.find(js.Dictionary(filter: _*), firstOnly = true).data()
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


    private[jsfacades] final class Fake[E: Scala2Js.MapConverter](entities: Seq[E]) extends ResultSet[E] {

      implicit val jsValueOrdering: Ordering[js.Any] = {
        new Ordering[js.Any] {
          override def compare(x: js.Any, y: js.Any): Int = {
            if (x.getClass == classOf[String]) {
              x.asInstanceOf[String] compareTo y.asInstanceOf[String]
            } else if (x.isInstanceOf[Int]) {
              x.asInstanceOf[Int] compareTo y.asInstanceOf[Int]
            } else {
              ???
            }
          }
        }
      }

      // **************** Intermediary operations **************** //
      override def find(filters: (String, js.Any)*) = new ResultSet.Fake(
        entities filter { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          filters.filter { case (k, v) => jsMap(k) != v }.isEmpty
        }
      )

      override def sort(propName: String, isDesc: Boolean) = {
        val newData = {
          val sorted = entities.sortBy { entity =>
            val jsMap = Scala2Js.toJsMap(entity)
            jsMap(propName)
          }
          if (isDesc) {
            sorted.reverse
          } else {
            sorted
          }
        }
        new ResultSet.Fake(newData)
      }

      override def limit(quantity: Int) = new ResultSet.Fake(
        entities.take(quantity)
      )

      // **************** Terminal operations **************** //
      override def data() = entities

      override def findOne(filters: (String, js.Any)*) = find(filters: _*).limit(1).data() match {
        case Seq(e) => Some(e)
        case Seq() => None
      }

      override def count() = entities.length
    }
  }
}
