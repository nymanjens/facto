package jsfacades

import common.LoggingUtils.logExceptions
import common.GuavaReplacement.Iterables.getOnlyElement
import common.ScalaUtils
import jsfacades.LokiJs.ResultSet.Filter
import jsfacades.LokiJs.Sorting.KeyWithDirection

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobal
import scala2js.Converters._
import scala2js.Scala2Js
import scala.collection.immutable.Seq
import scala.util.matching.Regex
import scala2js.Scala2Js.{Converter, Key}

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
    final def filterEqual[V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): ResultSet[E] =
      filter(Filter.equal(key, value))
    def filter(filter: Filter[E]): ResultSet[E]

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

    def fake[E: Scala2Js.MapConverter](entities: Seq[E]): ResultSet[E] = new ResultSet.Fake(entities)

    sealed trait Filter[E]
    object Filter {
      def nullFilter[E]: Filter[E] = NullFilter[E]()
      def equal[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): Filter[E] =
        Equal(key.name, Scala2Js.toJs(value))
      def notEqual[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): Filter[E] =
        NotEqual(key.name, Scala2Js.toJs(value))
      def greaterThan[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): Filter[E] =
        GreaterThan(key.name, Scala2Js.toJs(value))
      def lessThan[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E], value: V): Filter[E] =
        LessThan(key.name, Scala2Js.toJs(value))
      def anyOf[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E], values: Seq[V]): Filter[E] =
        AnyOf(key.name, Scala2Js.toJs(values))
      def noneOf[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E], values: Seq[V]): Filter[E] =
        NoneOf(key.name, Scala2Js.toJs(values))
      def containsIgnoreCase[E](key: Scala2Js.Key[String, E], substring: String): Filter[E] =
        ContainsIgnoreCase(key.name, substring)
      def doesntContainIgnoreCase[E](key: Scala2Js.Key[String, E], substring: String): Filter[E] =
        DoesntContainIgnoreCase(key.name, substring)
      def seqContains[E, V: Scala2Js.Converter](key: Scala2Js.Key[Seq[V], E], value: V): Filter[E] =
        SeqContains(key.name, Scala2Js.toJs(value))
      def seqDoesntContain[E, V: Scala2Js.Converter](key: Scala2Js.Key[Seq[V], E], value: V): Filter[E] =
        SeqDoesntContain(key.name, Scala2Js.toJs(value))
      def or[E](filters: Filter[E]*): Filter[E] = Or(Seq(filters: _*))
      def and[E](filters: Filter[E]*): Filter[E] = And(Seq(filters: _*))

      private[ResultSet] case class NullFilter[E]() extends Filter[E]
      private[ResultSet] case class Equal[E](keyName: String, value: js.Any) extends Filter[E]
      private[ResultSet] case class NotEqual[E](keyName: String, value: js.Any) extends Filter[E]
      private[ResultSet] case class GreaterThan[E](keyName: String, value: js.Any) extends Filter[E]
      private[ResultSet] case class LessThan[E](keyName: String, value: js.Any) extends Filter[E]
      private[ResultSet] case class AnyOf[E](keyName: String, values: js.Array[js.Any]) extends Filter[E]
      private[ResultSet] case class NoneOf[E](keyName: String, values: js.Array[js.Any]) extends Filter[E]
      private[ResultSet] case class ContainsIgnoreCase[E](keyName: String, substring: String)
          extends Filter[E]
      private[ResultSet] case class DoesntContainIgnoreCase[E](keyName: String, substring: String)
          extends Filter[E]
      private[ResultSet] case class SeqContains[E](keyName: String, value: js.Any) extends Filter[E]
      private[ResultSet] case class SeqDoesntContain[E](keyName: String, value: js.Any) extends Filter[E]
      private[ResultSet] case class Or[E](filters: Seq[Filter[E]]) extends Filter[E]
      private[ResultSet] case class And[E](filters: Seq[Filter[E]]) extends Filter[E]
    }

    private[jsfacades] final class Impl[E: Scala2Js.MapConverter](facade: ResultSetFacade)
        extends ResultSet[E] {

      // **************** Intermediary operations **************** //
      override def filter(filter: Filter[E]) = {
        def withModifier(modifier: String, keyName: String, value: js.Any): js.Dictionary[js.Any] = {
          js.Dictionary(keyName -> js.Dictionary(modifier -> value))
        }
        def toFilterDictionary(filter: Filter[E]): js.Dictionary[js.Any] = filter match {
          case Filter.NullFilter() => js.Dictionary()
          case Filter.Equal(key, value) => withModifier("$eq", key, value)
          case Filter.NotEqual(key, value) => withModifier("$ne", key, value)
          case Filter.GreaterThan(key, value) => withModifier("$gt", key, value)
          case Filter.LessThan(key, value) => withModifier("$lt", key, value)
          case Filter.AnyOf(key, values) => withModifier("$in", key, values)
          case Filter.NoneOf(key, values) => withModifier("$nin", key, values)
          case Filter.ContainsIgnoreCase(key, substring) =>
            withModifier("$regex", key, js.Array(Regex.quote(substring), "i"))
          case Filter.DoesntContainIgnoreCase(key, substring) =>
            withModifier("$regex", key, js.Array(s"""^((?!${Regex.quote(substring)})[\\s\\S])*$$""", "i"))
          case Filter.SeqContains(key, value) => withModifier("$contains", key, value)
          case Filter.SeqDoesntContain(key, value) => withModifier("$containsNone", key, value)
          case Filter.Or(filters) => js.Dictionary("$or" -> filters.map(toFilterDictionary).toJSArray)
          case Filter.And(filters) => js.Dictionary("$and" -> filters.map(toFilterDictionary).toJSArray)
        }
        val filterDictionary = toFilterDictionary(filter)
        new ResultSet.Impl[E](facade.find(filterDictionary))
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
      override def filter(filter: Filter[E]) = {
        def applyFilter(jsMap: js.Dictionary[js.Any], filter: Filter[E]): Boolean = filter match {
          case Filter.NullFilter() => true
          case Filter.Equal(key, value) => jsMap(key) == value
          case Filter.NotEqual(key, value) => jsMap(key) != value
          case Filter.GreaterThan(key, value) => jsValueOrdering.gt(jsMap(key), value)
          case Filter.LessThan(key, value) => jsValueOrdering.lt(jsMap(key), value)
          case Filter.AnyOf(key, values) => values.contains(jsMap(key))
          case Filter.NoneOf(key, values) => !(values contains jsMap(key))
          case Filter.ContainsIgnoreCase(key, substring) =>
            jsMap(key).toString.toLowerCase.contains(substring.toLowerCase)
          case Filter.DoesntContainIgnoreCase(key, substring) =>
            !jsMap(key).toString.toLowerCase.contains(substring.toLowerCase)
          case Filter.SeqContains(key, value) =>
            jsMap(key).asInstanceOf[js.Array[js.Any]] contains Scala2Js.toJs(value)
          case Filter.SeqDoesntContain(key, value) =>
            !(jsMap(key).asInstanceOf[js.Array[js.Any]] contains Scala2Js.toJs(value))
          case Filter.Or(filters) => filters.exists(applyFilter(jsMap, _))
          case Filter.And(filters) => filters.forall(applyFilter(jsMap, _))
        }

        new ResultSet.Fake(entities.filter { entity =>
          val jsMap = Scala2Js.toJsMap(entity)
          applyFilter(jsMap, filter)
        })
      }

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
        filterEqual(key, value).limit(1).data() match {
          case Seq(e) => Some(e)
          case Seq() => None
        }

      override def count() = entities.length
    }
  }
}
