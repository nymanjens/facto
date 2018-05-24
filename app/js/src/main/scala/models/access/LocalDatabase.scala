package models.access

import common.ScalaUtils.visibleForTesting
import common.testing.TestObjects
import jsfacades.LokiJs.FilterFactory.Operation
import jsfacades.{CryptoJs, LokiJs}
import models.Entity
import models.access.webworker.LocalDatabaseWebWorkerApi
import models.access.webworker.LocalDatabaseWebWorkerApi.{LokiQuery, WriteOperation}
import models.modification.EntityType.{
  BalanceCheckType,
  ExchangeRateMeasurementType,
  TransactionGroupType,
  TransactionType,
  UserType
}
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.matching.Regex
import scala2js.Converters._
import scala2js.Scala2Js

/** Client-side persistence layer. */
@visibleForTesting
trait LocalDatabase {
  // **************** Getters ****************//
  def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E]
  def pendingModifications(): Future[Seq[EntityModification]]
  def getSingletonValue[V](key: SingletonKey[V]): Future[Option[V]]
  def isEmpty: Future[Boolean]

  // **************** Setters ****************//
  /**
    * Applies given modification in memory but doesn't persist it in the browser's storage (call `save()` to do this).
    *
    * @return true if the in memory database changed as a result of this method
    */
  def applyModifications(modifications: Seq[EntityModification]): Future[Boolean]
  def addAll[E <: Entity: EntityType](entities: Seq[E]): Future[Unit]

  def addPendingModifications(modifications: Seq[EntityModification]): Future[Unit]
  def removePendingModifications(modifications: Seq[EntityModification]): Future[Unit]

  /** Sets given singleton value in memory but doesn't persist it in the browser's storage (call `save()` to do this). */
  def setSingletonValue[V](key: SingletonKey[V], value: V): Future[Unit]

  /** Persists all previously made changes to the browser's storage. */
  def save(): Future[Unit]

  /** Removes all data and resets its configuration. */
  def resetAndInitialize(): Future[Unit]
}

@visibleForTesting
object LocalDatabase {

  def create(encryptionSecret: String = "")(
      implicit webWorker: LocalDatabaseWebWorkerApi): Future[LocalDatabase] = async {
    await(webWorker.create(dbName = "facto-db", encryptionSecret = encryptionSecret, inMemory = false))
    new Impl()
  }

  def createStoredForTests(encryptionSecret: String = "")(
      implicit webWorker: LocalDatabaseWebWorkerApi): Future[LocalDatabase] = async {
    await(webWorker.create(dbName = "test-db", encryptionSecret = encryptionSecret, inMemory = false))
    new Impl()
  }

  def createInMemoryForTests(encryptionSecret: String = "")(
      implicit webWorker: LocalDatabaseWebWorkerApi): Future[LocalDatabase] = async {
    await(webWorker.create(dbName = "facto-db", encryptionSecret = encryptionSecret, inMemory = true))
    new Impl()
  }

  private case class Singleton(key: String, value: js.Any)

  private object Singleton {
    implicit object Converter extends Scala2Js.MapConverter[Singleton] {
      override def toJs(singleton: Singleton) = {
        js.Dictionary[js.Any]("id" -> singleton.key, "value" -> singleton.value)
      }
      override def toScala(dict: js.Dictionary[js.Any]) = {
        def getRequired[V: Scala2Js.Converter](fieldName: String) = {
          require(dict.contains(fieldName), s"Key ${fieldName} is missing from ${js.JSON.stringify(dict)}")
          Scala2Js.toScala[V](dict(fieldName))
        }
        Singleton(key = getRequired[String]("id"), value = getRequired[js.Any]("value"))
      }
    }
  }

  private case class ModificationWithId(modification: EntityModification) {
    def id: Long = modification.hashCode()
  }

  private object ModificationWithId {
    implicit object Converter extends Scala2Js.MapConverter[ModificationWithId] {
      override def toJs(modificationWithId: ModificationWithId) = {
        js.Dictionary[js.Any](
          "id" -> Scala2Js.toJs(modificationWithId.id),
          "modification" -> Scala2Js.toJs(modificationWithId.modification))
      }
      override def toScala(dict: js.Dictionary[js.Any]) = {
        def getRequired[V: Scala2Js.Converter](fieldName: String) = {
          require(dict.contains(fieldName), s"Key ${fieldName} is missing from ${js.JSON.stringify(dict)}")
          Scala2Js.toScala[V](dict(fieldName))
        }
        ModificationWithId(getRequired[EntityModification]("modification"))
      }
    }
  }

  private final class Impl(implicit webWorker: LocalDatabaseWebWorkerApi) extends LocalDatabase {
    // **************** Getters ****************//
    def queryExecutor[E <: Entity: EntityType]() =
      new DbQueryExecutor.Async[E] {
        override def data(dbQuery: DbQuery[E]) =
          webWorker.executeDataQuery(toLokiQuery(dbQuery)).map(_.map(Scala2Js.toScala[E]))
        override def count(dbQuery: DbQuery[E]) = webWorker.executeCountQuery(toLokiQuery(dbQuery))

        private def toLokiQuery(dbQuery: DbQuery[E]): LokiQuery = LokiQuery(
          collectionName = collectionNameOf(implicitly[EntityType[E]]),
          filter = toLokiJsFilter(dbQuery.filter),
          sorting = dbQuery.sorting.map(sorting =>
            LokiJs.SortingFactory.keysWithDirection(sorting.fieldsWithDirection.map {
              case DbQuery.Sorting.FieldWithDirection(field, isDesc) =>
                LokiJs.SortingFactory.KeyWithDirection(field.name, isDesc)
            })),
          limit = dbQuery.limit
        )

        private def toLokiJsFilter(filter: DbQuery.Filter[E]): Option[js.Dictionary[js.Any]] = {
          def rawKeyValueFilter(operation: Operation,
                                field: ModelField[_, E],
                                jsValue: js.Any): Option[js.Dictionary[js.Any]] =
            Some(LokiJs.FilterFactory.keyValueFilter(operation, field.name, jsValue))
          def keyValueFilter[V](operation: Operation,
                                field: ModelField[V, E],
                                value: V): Option[js.Dictionary[js.Any]] =
            rawKeyValueFilter(operation, field, Scala2Js.toJs(value, field))

          filter match {
            case DbQuery.Filter.NullFilter()           => None
            case DbQuery.Filter.Equal(field, value)    => keyValueFilter(Operation.Equal, field, value)
            case DbQuery.Filter.NotEqual(field, value) => keyValueFilter(Operation.NotEqual, field, value)
            case DbQuery.Filter.GreaterThan(field, value) =>
              keyValueFilter(Operation.GreaterThan, field, value)
            case DbQuery.Filter.GreaterOrEqualThan(field, value) =>
              keyValueFilter(Operation.GreaterOrEqualThan, field, value)
            case DbQuery.Filter.LessThan(field, value) => keyValueFilter(Operation.LessThan, field, value)
            case DbQuery.Filter.AnyOf(field, values) =>
              rawKeyValueFilter(Operation.AnyOf, field, values.map(Scala2Js.toJs(_, field)).toJSArray)
            case DbQuery.Filter.NoneOf(field, values) =>
              rawKeyValueFilter(Operation.NoneOf, field, values.map(Scala2Js.toJs(_, field)).toJSArray)
            case DbQuery.Filter.ContainsIgnoreCase(field, substring) =>
              rawKeyValueFilter(Operation.Regex, field, js.Array(Regex.quote(substring), "i"))
            case DbQuery.Filter.DoesntContainIgnoreCase(field, substring) =>
              rawKeyValueFilter(Operation.Regex, field, js.Array(s"""^((?!${Regex
                .quote(substring)})[\\s\\S])*$$""", "i"))
            case DbQuery.Filter.SeqContains(field, value) =>
              rawKeyValueFilter(Operation.Contains, field, Scala2Js.toJs(value))
            case DbQuery.Filter.SeqDoesntContain(field, value) =>
              rawKeyValueFilter(Operation.ContainsNone, field, Scala2Js.toJs(value))
            case DbQuery.Filter.Or(filters) =>
              Some(LokiJs.FilterFactory.aggregateFilter(Operation.Or, filters.flatMap(toLokiJsFilter)))
            case DbQuery.Filter.And(filters) =>
              Some(LokiJs.FilterFactory.aggregateFilter(Operation.And, filters.flatMap(toLokiJsFilter)))
          }
        }
      }

    override def pendingModifications(): Future[Seq[EntityModification]] = async {
      val allData =
        await(webWorker.executeDataQuery(LokiQuery(collectionName = pendingModificationsCollectionName)))
      for (data <- allData) yield Scala2Js.toScala[ModificationWithId](data).modification
    }

    override def getSingletonValue[V](key: SingletonKey[V]) = async {
      implicit val converter = key.valueConverter
      val results = await(
        webWorker.executeDataQuery(
          LokiQuery(
            collectionName = singletonsCollectionName,
            filter = Some(LokiJs.FilterFactory.keyValueFilter(Operation.Equal, "id", key.name)),
            limit = Some(1))))
      val value = results match {
        case Seq(v) => Some(v)
        case Seq()  => None
      }
      value.map(Scala2Js.toScala[Singleton]).map(v => Scala2Js.toScala[V](v.value))
    }

    override def isEmpty = async {
      val numbers = await(Future.sequence {
        for (collectionName <- allCollectionNames)
          yield webWorker.executeCountQuery(LokiQuery(collectionName = collectionName))
      })
      numbers.forall(_ == 0)
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]) = async {
      await(webWorker.applyWriteOperations(modifications map {
        case modification @ EntityModification.Add(entity) =>
          implicit val entityType = modification.entityType
          WriteOperation.Insert(collectionNameOf(entityType), Scala2Js.toJsMap(entity))
        case modification @ EntityModification.Update(updatedEntity) =>
          implicit val entityType = modification.entityType
          WriteOperation.Update(collectionNameOf(entityType), Scala2Js.toJsMap(updatedEntity))
        case modification @ EntityModification.Remove(id) =>
          val entityType = modification.entityType
          WriteOperation.Remove(collectionNameOf(entityType), Scala2Js.toJs(id))
      }))
    }

    override def addAll[E <: Entity: EntityType](entities: Seq[E]) = async {
      val collectionName = collectionNameOf(implicitly[EntityType[E]])
      await(
        webWorker.applyWriteOperations(
          for (entity <- entities) yield WriteOperation.Insert(collectionName, Scala2Js.toJsMap(entity))
        ))
    }

    override def addPendingModifications(modifications: Seq[EntityModification]): Future[Unit] = async {
      await(
        webWorker.applyWriteOperations(
          for (modification <- modifications)
            yield
              WriteOperation
                .Insert(
                  pendingModificationsCollectionName,
                  Scala2Js.toJsMap(ModificationWithId(modification)))
        ))
    }

    override def removePendingModifications(modifications: Seq[EntityModification]): Future[Unit] = async {
      await(
        webWorker.applyWriteOperations(
          for (modification <- modifications)
            yield
              WriteOperation
                .Remove(
                  pendingModificationsCollectionName,
                  Scala2Js.toJs(ModificationWithId(modification).id))
        ))
    }

    override def setSingletonValue[V](key: SingletonKey[V], value: V) = async {
      implicit val converter = key.valueConverter
      await(
        webWorker.applyWriteOperations(Seq(
          WriteOperation.Remove(singletonsCollectionName, id = key.name),
          WriteOperation.Insert(
            singletonsCollectionName,
            Scala2Js.toJsMap(Singleton(
              key = key.name,
              value = Scala2Js.toJs(value)
            )))
        )))
    }

    override def save(): Future[Unit] = async {
      console.log("  Saving database...")
      await(webWorker.applyWriteOperations(Seq(WriteOperation.SaveDatabase)))
      console.log("  Saving database done.")
    }

    override def resetAndInitialize(): Future[Unit] = async {
      console.log("  Resetting database...")
      await(
        webWorker.applyWriteOperations(
          Seq() ++
            (for (collectionName <- allCollectionNames)
              yield WriteOperation.RemoveCollection(collectionName)) ++
            (for (entityType <- EntityType.values)
              yield
                WriteOperation.AddCollection(
                  collectionNameOf(entityType),
                  uniqueIndices = Seq("id"),
                  indices = secondaryIndices(entityType).map(_.name))) :+
            WriteOperation
              .AddCollection(singletonsCollectionName, uniqueIndices = Seq("id"), indices = Seq()) :+
            WriteOperation
              .AddCollection(pendingModificationsCollectionName, uniqueIndices = Seq("id"), indices = Seq())))
      console.log("  Resetting database done.")
    }

    // **************** Private helper methods ****************//
    private def collectionNameOf(entityType: EntityType.any): String = s"entities_${entityType.name}"
    private val singletonsCollectionName = "singletons"
    private val pendingModificationsCollectionName = "pendingModifications"
    private def allCollectionNames: Seq[String] =
      EntityType.values.map(collectionNameOf) :+ singletonsCollectionName :+ pendingModificationsCollectionName

    private def secondaryIndices(entityType: EntityType.any): Seq[ModelField[_, _]] = entityType match {
      case TransactionType =>
        Seq(
          ModelField.Transaction.transactionGroupId,
          ModelField.Transaction.moneyReservoirCode,
          ModelField.Transaction.beneficiaryAccountCode)
      case TransactionGroupType        => Seq()
      case BalanceCheckType            => Seq()
      case ExchangeRateMeasurementType => Seq()
      case UserType                    => Seq()
    }
  }
}
