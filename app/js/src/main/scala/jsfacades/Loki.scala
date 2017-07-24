package jsfacades

import common.GuavaReplacement.Iterables.getOnlyElement
import common.ScalaUtils
import jsfacades.Loki.Sorting.PropNameWithDirection

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSName}
import scala.scalajs.js.JSConverters._
import scala2js.Converters._
import scala2js.Scala2Js

object Loki {
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
    def persistent(dbName: String): Database = {
      new Database(new DatabaseFacade(dbName, js.Dictionary("adapter" -> new IndexedAdapter(dbName))))
    }

    def inMemoryForTests(dbName: String): Database = {
      new Database(
        new DatabaseFacade(dbName, js.Dictionary("adapter" -> new MemoryAdapter(), "env" -> "BROWSER")))
    }
  }

  @JSGlobal("LokiIndexedAdapter")
  @js.native
  final class IndexedAdapter(name: String) extends js.Object

  @JSGlobal("loki.LokiMemoryAdapter")
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
    def findAndRemove(filter: (String, js.Any)): Unit = facade.findAndRemove(js.Dictionary(filter))
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
    /**
      * Finds an exact match for plain filter values or applies other kinds of matching
      * when modifiers are used (see `ResultSet`).
      */
    def find(filter: (String, js.Any)): ResultSet[E]

    /**
      * Loose evaluation for user to sort based on a property names. Sorting based on the same lt/gt helper
      * functions used for binary indices.
      */
    def sort(sorting: Loki.Sorting): ResultSet[E]
    def limit(quantity: Int): ResultSet[E]

    // **************** Terminal operations **************** //
    /**
      * Finds an exact match for plain filter values or applies other kinds of matching
      * when modifiers are used (see `ResultSet`).
      */
    def findOne(filter: (String, js.Any)): Option[E]
    def data(): Seq[E]
    def count(): Int
  }

  case class Sorting private (private[Loki] val propNamesWithDirection: Seq[Sorting.PropNameWithDirection]) {
    def thenAscBy(propName: String): Sorting = thenBy(propName, isDesc = false)
    def thenDescBy(propName: String): Sorting = thenBy(propName, isDesc = true)
    def thenBy(propName: String, isDesc: Boolean): Sorting =
      Sorting(propNamesWithDirection :+ PropNameWithDirection(propName, isDesc = isDesc))
  }
  object Sorting {
    def ascBy(propName: String): Sorting = by(propName, isDesc = false)
    def descBy(propName: String): Sorting = by(propName, isDesc = true)
    def by(propName: String, isDesc: Boolean): Sorting =
      Sorting(Seq(PropNameWithDirection(propName, isDesc = isDesc)))

    private[Loki] case class PropNameWithDirection(propName: String, isDesc: Boolean)
  }

  object ResultSet {

    def empty[E: Scala2Js.MapConverter]: ResultSet[E] = new ResultSet.Fake(Seq())

    /** To be used as ResultSet find values, e.g. `.find("createdDate" -> greaterThan(myDate))`. */
    def greaterThan[T: Scala2Js.Converter](value: T): js.Dictionary[js.Any] =
      js.Dictionary("$gt" -> Scala2Js.toJs(value))
    def lessThan[T: Scala2Js.Converter](value: T): js.Dictionary[js.Any] =
      js.Dictionary("$lt" -> Scala2Js.toJs(value))

    private[jsfacades] final class Impl[E: Scala2Js.MapConverter](facade: ResultSetFacade)
        extends ResultSet[E] {

      // **************** Intermediary operations **************** //
      override def find(filter: (String, js.Any)) = {
        new ResultSet.Impl[E](facade.find(js.Dictionary(filter)))
      }

      override def sort(sorting: Loki.Sorting) = {
        val properties: js.Array[js.Array[js.Any]] = {
          val result: Seq[js.Array[js.Any]] = sorting.propNamesWithDirection map
            (nameWithDirection => js.Array[js.Any](nameWithDirection.propName, nameWithDirection.isDesc))
          result.toJSArray
        }
        new ResultSet.Impl[E](facade.compoundsort(properties))
      }

      override def limit(quantity: Int) = {
        new ResultSet.Impl[E](facade.limit(quantity))
      }

      // **************** Terminal operations **************** //
      override def findOne(filter: (String, js.Any)) = {
        val data = facade.find(js.Dictionary(filter), firstOnly = true).data()
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

      implicit private val jsValueOrdering: Ordering[js.Any] = {
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
      override def find(filter: (String, js.Any)) = new ResultSet.Fake(
        entities.filter { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          filter match {
            case (k, v) => jsMap(k) == v
          }
        }
      )

      override def sort(sorting: Loki.Sorting) = {
        val newData: Seq[E] = {
          entities.sortWith(lt = (lhs, rhs) => {
            val lhsMap = Scala2Js.toJsMap(lhs)
            val rhsMap = Scala2Js.toJsMap(rhs)
            val results = for {
              Sorting.PropNameWithDirection(propName, isDesc) <- sorting.propNamesWithDirection
              if !jsValueOrdering.equiv(lhsMap(propName), rhsMap(propName))
            } yield {
              if (jsValueOrdering.lt(lhsMap(propName), rhsMap(propName))) {
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

      override def findOne(filter: (String, js.Any)) = find(filter).limit(1).data() match {
        case Seq(e) => Some(e)
        case Seq() => None
      }

      override def count() = entities.length
    }
  }
}
