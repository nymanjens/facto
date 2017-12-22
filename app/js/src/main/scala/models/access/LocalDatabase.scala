package models.access

import common.ScalaUtils.visibleForTesting
import jsfacades.LokiJs.Filter.Operation
import jsfacades.{CryptoJs, LokiJs}
import models.Entity
import models.access.DbResultSet.DbQueryExecutor
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.matching.Regex
import scala2js.Converters._
import scala2js.Scala2Js

@visibleForTesting
trait LocalDatabase {
  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): DbResultSet[E]
  def getSingletonValue[V](key: SingletonKey[V]): Option[V]
  def isEmpty: Boolean

  // **************** Setters ****************//
  /**
    * Applies given modification in memory but doesn't persist it in the browser's storage (call `save()` to do this).
    *
    * @return true if the in memory database changed as a result of this method
    */
  def applyModifications(modifications: Seq[EntityModification]): Boolean
  def addAll[E <: Entity: EntityType](entities: Seq[E]): Unit

  /** Sets given singleton value in memory but doesn't persist it in the browser's storage (call `save()` to do this). */
  def setSingletonValue[V](key: SingletonKey[V], value: V): Unit

  /** Persists all previously made changes to the browser's storage. */
  def save(): Future[Unit]
  def clear(): Future[Unit]
}

@visibleForTesting
object LocalDatabase {

  def createFuture(encryptionSecret: String = ""): Future[LocalDatabase] = async {
    val lokiDb: LokiJs.Database = LokiJs.Database.persistent(
      "facto-db",
      persistedStringCodex =
        if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
        else new EncryptingCodex(encryptionSecret))
    await(lokiDb.loadDatabase())
    new Impl(lokiDb)
  }

  def createStoredForTests(encryptionSecret: String = ""): Future[LocalDatabase] = async {
    val lokiDb: LokiJs.Database = LokiJs.Database.persistent(
      "test-db",
      persistedStringCodex =
        if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
        else new EncryptingCodex(encryptionSecret))
    await(lokiDb.loadDatabase())
    new Impl(lokiDb)
  }

  def createInMemoryForTests(encryptionSecret: String = ""): Future[LocalDatabase] = async {
    val lokiDb: LokiJs.Database =
      LokiJs.Database.inMemoryForTests(
        "facto-db",
        persistedStringCodex =
          if (encryptionSecret.isEmpty) LokiJs.PersistedStringCodex.NullCodex
          else new EncryptingCodex(encryptionSecret))
    await(lokiDb.loadDatabase())
    new Impl(lokiDb)
  }

  private case class Singleton(key: String, value: js.Any)

  private object Singleton {
    implicit object Converter extends Scala2Js.MapConverter[Singleton] {
      override def toJs(singleton: Singleton) = {
        js.Dictionary[js.Any]("key" -> singleton.key, "value" -> singleton.value)
      }
      override def toScala(dict: js.Dictionary[js.Any]) = {
        def getRequired[V: Scala2Js.Converter](fieldName: String) = {
          require(dict.contains(fieldName), s"Key ${fieldName} is missing from ${js.JSON.stringify(dict)}")
          Scala2Js.toScala[V](dict(fieldName))
        }
        Singleton(key = getRequired[String]("key"), value = getRequired[js.Any]("value"))
      }
    }
  }

  private final class EncryptingCodex(secret: String) extends LokiJs.PersistedStringCodex {
    private val decodedPrefix = "DECODED"

    override def encodeBeforeSave(dbString: String) = {
      val millis1 = System.currentTimeMillis()
      println(s"  Encrypting ${dbString.length / 1e6}Mb String...")
      val result =
        CryptoJs.RC4Drop.encrypt(stringToEncrypt = decodedPrefix + dbString, password = secret).toString()
      val millis2 = System.currentTimeMillis()
      println(s"  Encrypting ${dbString.length / 1e6}Mb String: Done after ${(millis2 - millis1) / 1e3}s")
      result
    }

    override def decodeAfterLoad(encodedString: String) = {
      val millis1 = System.currentTimeMillis()
      println(s"  Decrypting ${encodedString.length / 1e6}Mb String...")
      val decoded =
        try {
          CryptoJs.RC4Drop
            .decrypt(stringToDecrypt = encodedString, password = secret)
            .toString(CryptoJs.Encoding.Utf8)
        } catch {
          case t: Throwable =>
            println(s"  Caught exception while decoding database string: $t")
            ""
        }
      val millis2 = System.currentTimeMillis()
      println(
        s"  Decrypting ${encodedString.length / 1e6}Mb String: Done after ${(millis2 - millis1) / 1e3}s")
      if (decoded.startsWith(decodedPrefix)) {
        Some(decoded.substring(decodedPrefix.length))
      } else {
        println(s"  Failed to decode database string: ${encodedString.substring(0, 10)}")
        None
      }
    }
  }

  private final class Impl(val lokiDb: LokiJs.Database) extends LocalDatabase {
    val entityCollections: Map[EntityType.any, LokiJs.Collection[_]] = {
      def getOrAddCollection[E <: Entity](implicit entityType: EntityType[E]): LokiJs.Collection[E] = {
        // TODO: Add primary indices
        lokiDb.getOrAddCollection[E](s"entities_${entityType.name}")
      }
      for (entityType <- EntityType.values) yield {
        entityType -> getOrAddCollection(entityType)
      }
    }.toMap
    // TODO: Add primary index on key
    val singletonCollection: LokiJs.Collection[Singleton] =
      lokiDb.getOrAddCollection[Singleton](s"singletons")

    // **************** Getters ****************//
    override def newQuery[E <: Entity: EntityType](): DbResultSet[E] =
      DbResultSet.fromExecutor(new DbQueryExecutor[E] {
        override def data(dbQuery: DbQuery[E]) = lokiResultSet(dbQuery).data()
        override def count(dbQuery: DbQuery[E]) = lokiResultSet(dbQuery).count()

        private def lokiResultSet(dbQuery: DbQuery[E]): LokiJs.ResultSet[E] = {
          var resultSet = entityCollectionForImplicitType[E].chain()
          for (filter <- toLokiJsFilter(dbQuery.filter)) {
            resultSet = resultSet.filter(filter)
          }
          for (sorting <- dbQuery.sorting) {
            resultSet = resultSet.sort(LokiJs.Sorting(sorting.fieldsWithDirection.map {
              case DbQuery.Sorting.FieldWithDirection(field, isDesc) =>
                LokiJs.Sorting.KeyWithDirection(field.name, isDesc)
            }))
          }
          for (limit <- dbQuery.limit) {
            resultSet = resultSet.limit(limit)
          }
          resultSet
        }

        private def toLokiJsFilter(filter: DbQuery.Filter[E]): Option[LokiJs.Filter] = {
          def keyValueFilter[V](operation: Operation,
                                field: ModelField[V, E],
                                value: V): Option[LokiJs.Filter] = {
            val (fieldName, jsValue) = Scala2Js.toJsPair(field -> value)
            Some(LokiJs.Filter.KeyValueFilter(operation, fieldName, jsValue))
          }
          filter match {
            case DbQuery.Filter.NullFilter() => None
            case DbQuery.Filter.Equal(field, value) => keyValueFilter(Operation.Equal, field, value)
            case DbQuery.Filter.NotEqual(field, value) => keyValueFilter(Operation.NotEqual, field, value)
            case DbQuery.Filter.GreaterThan(field, value) =>
              keyValueFilter(Operation.GreaterThan, field, value)
            case DbQuery.Filter.GreaterOrEqualThan(field, value) =>
              keyValueFilter(Operation.GreaterOrEqualThan, field, value)
            case DbQuery.Filter.LessThan(field, value) => keyValueFilter(Operation.LessThan, field, value)
            case DbQuery.Filter.AnyOf(field, values) => keyValueFilter(Operation.AnyOf, field, values)
            case DbQuery.Filter.NoneOf(field, values) => keyValueFilter(Operation.NoneOf, field, values)
            case DbQuery.Filter.ContainsIgnoreCase(field, substring) =>
              Some(
                LokiJs.Filter
                  .KeyValueFilter(Operation.Regex, field.name, js.Array(Regex.quote(substring), "i")))
            case DbQuery.Filter.DoesntContainIgnoreCase(field, substring) =>
              Some(LokiJs.Filter.KeyValueFilter(Operation.Regex, field.name, js.Array(s"""^((?!${Regex
                .quote(substring)})[\\s\\S])*$$""", "i")))
            case DbQuery.Filter.SeqContains(field, value) =>
              Some(LokiJs.Filter.KeyValueFilter(Operation.Contains, field.name, Scala2Js.toJs(value)))
            case DbQuery.Filter.SeqDoesntContain(field, value) =>
              Some(LokiJs.Filter.KeyValueFilter(Operation.ContainsNone, field.name, Scala2Js.toJs(value)))
            case DbQuery.Filter.Or(filters) =>
              Some(LokiJs.Filter.AggregateFilter(Operation.Or, filters.flatMap(toLokiJsFilter)))
            case DbQuery.Filter.And(filters) =>
              Some(LokiJs.Filter.AggregateFilter(Operation.And, filters.flatMap(toLokiJsFilter)))
          }
        }
      })

    override def getSingletonValue[V](key: SingletonKey[V]): Option[V] = {
      implicit val converter = key.valueConverter
      val value =
        singletonCollection
          .chain()
          .filter(LokiJs.Filter.KeyValueFilter(LokiJs.Filter.Operation.Equal, "name", key.name))
          .limit(1)
          .data() match {
          case Seq(v) => Some(v)
          case Seq() => None
        }
      value.map(v => Scala2Js.toScala[V](v.value))
    }

    override def isEmpty: Boolean = {
      allCollections.toStream.filter(_.chain().count() != 0).isEmpty
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]): Boolean = {
      val modificationsCausedChange =
        for (modification <- modifications) yield {
          modification match {
            case addModification: EntityModification.Add[_] =>
              def add[E <: Entity](modification: EntityModification.Add[E]): Boolean = {
                implicit val _ = modification.entityType
                newQuery[E]().findOne(Fields.id, modification.entity.id) match {
                  case Some(entity) => false // do nothing
                  case None =>
                    entityCollectionForImplicitType[E].insert(modification.entity)
                    true
                }
              }
              add(addModification)
            case updateModification: EntityModification.Update[_] =>
              def update[E <: Entity](modification: EntityModification.Update[E]): Boolean = {
                implicit val _ = modification.entityType
                newQuery[E]().findOne(Fields.id, modification.updatedEntity.id) match {
                  case Some(_) =>
                    // Not using collection.update() because it requires a sync
                    entityCollectionForImplicitType[E]
                      .findAndRemove(Fields.id.name, modification.updatedEntity.id)
                    entityCollectionForImplicitType[E].insert(modification.updatedEntity)
                    true
                  case None => false // do nothing
                }
              }
              update(updateModification)
            case removeModification: EntityModification.Remove[_] =>
              def remove[E <: Entity](modification: EntityModification.Remove[E]): Boolean = {
                implicit val _ = modification.entityType
                newQuery[E]().findOne(Fields.id, modification.entityId) match {
                  case Some(entity) =>
                    entityCollectionForImplicitType.findAndRemove(Fields.id.name, modification.entityId)
                    true
                  case None => false // do nothing
                }
              }
              remove(removeModification)
          }
        }
      modificationsCausedChange contains true
    }

    override def addAll[E <: Entity: EntityType](entities: Seq[E]): Unit = {
      for (entity <- entities) {
        newQuery[E]().findOne(Fields.id, entity.id) match {
          case Some(_) => // do nothing
          case None => entityCollectionForImplicitType.insert(entity)
        }
      }
    }

    override def setSingletonValue[V](key: SingletonKey[V], value: V): Unit = {
      implicit val converter = key.valueConverter
      singletonCollection.findAndRemove("key", key.name)
      singletonCollection.insert(
        Singleton(
          key = key.name,
          value = Scala2Js.toJs(value)
        ))
    }

    override def save(): Future[Unit] = async {
      println("  Saving database...")
      await(lokiDb.saveDatabase())
      println("  Saving database done.")
    }

    override def clear(): Future[Unit] = async {
      println("  Clearing database...")
      for (collection <- allCollections) {
        collection.clear()
      }
      await(lokiDb.saveDatabase())
      println("  Clearing database done.")
    }

    // **************** Private helper methods ****************//
    private def allCollections: Seq[LokiJs.Collection[_]] =
      entityCollections.values.toList :+ singletonCollection

    private def entityCollectionForImplicitType[E <: Entity: EntityType]: LokiJs.Collection[E] = {
      entityCollections(implicitly[EntityType[E]]).asInstanceOf[LokiJs.Collection[E]]
    }
  }
}
