package hydro.models.access

import hydro.models.access.DbQueryImplicits._
import app.models.access.ModelFields
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.modification.EntityTypes
import app.scala2js.AppConverters._
import hydro.common.SerializingTaskQueue
import hydro.jsfacades.LokiJs
import hydro.jsfacades.LokiJs.FilterFactory.Operation
import hydro.models.Entity
import hydro.models.access.LocalDatabaseImpl.ModificationWithId
import hydro.models.access.LocalDatabaseImpl.SecondaryIndexFunction
import hydro.models.access.LocalDatabaseImpl.Singleton
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.LokiQuery
import hydro.models.access.webworker.LocalDatabaseWebWorkerApi.WriteOperation
import hydro.models.UpdatableEntity
import hydro.scala2js.Scala2Js
import hydro.scala2js.StandardConverters._
import org.scalajs.dom.console

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.matching.Regex

private final class LocalDatabaseImpl(
    implicit webWorker: LocalDatabaseWebWorkerApi,
    secondaryIndexFunction: SecondaryIndexFunction,
) extends LocalDatabase {

  private val serializingWriteQueue = SerializingTaskQueue.create()

  // Scala compiler bug workaround: Declare implicit converters
  implicit private val modificationWithIdConverter = ModificationWithId.Converter
  implicit private val singletonConverter = Singleton.Converter

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
        def rawKeyValueFilter(
            operation: Operation,
            field: ModelField[_, E],
            jsValue: js.Any,
        ): Option[js.Dictionary[js.Any]] =
          Some(LokiJs.FilterFactory.keyValueFilter(operation, field.name, jsValue))
        def keyValueFilter[V](
            operation: Operation,
            field: ModelField[V, E],
            value: V,
        ): Option[js.Dictionary[js.Any]] =
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
  override def applyModifications(modifications: Seq[EntityModification]) = serializingWriteQueue.schedule {
    async {
      val updatesToExistingEntityMap: Map[EntityModification, Option[Entity]] = await(
        Future
          .sequence(
            modifications
              .filter(m => m.isInstanceOf[EntityModification.Update[_]])
              .map { m =>
                def inner[E <: Entity: EntityType] =
                  DbResultSet
                    .fromExecutor(queryExecutor[E]())
                    .findOne(ModelFields.id[E] === m.entityId)
                    .map(e => m -> e)
                inner(m.entityType)
              })).toMap

      webWorker.applyWriteOperations(modifications flatMap {
        case modification @ EntityModification.Add(entity) =>
          implicit val entityType = modification.entityType
          Some(WriteOperation.Insert(collectionNameOf(entityType), Scala2Js.toJsMap(entity)))
        case modification @ EntityModification.Update(updatedEntity) =>
          def updateInner[E <: UpdatableEntity] = {
            implicit val entityType = modification.entityType.asInstanceOf[EntityType[E]]
            val maybeExistingEntity = updatesToExistingEntityMap(modification).asInstanceOf[Option[E]]

            maybeExistingEntity match {
              case Some(existingEntity) =>
                val mergedEntity = UpdatableEntity.merge(existingEntity, updatedEntity.asInstanceOf[E])
                Some(WriteOperation.Update(collectionNameOf(entityType), Scala2Js.toJsMap(mergedEntity)))
              case None => // Do nothing (no upsert)
                None
            }
          }
          updateInner
        case modification @ EntityModification.Remove(id) =>
          val entityType = modification.entityType
          Some(WriteOperation.Remove(collectionNameOf(entityType), Scala2Js.toJs(id)))
      })
    }
  }

  override def addAll[E <: Entity: EntityType](entities: Seq[E]) = serializingWriteQueue.schedule {
    val collectionName = collectionNameOf(implicitly[EntityType[E]])
    webWorker.applyWriteOperations(
      for (entity <- entities) yield WriteOperation.Insert(collectionName, Scala2Js.toJsMap(entity)))
  }

  override def addPendingModifications(modifications: Seq[EntityModification]): Future[Unit] =
    serializingWriteQueue.schedule {
      webWorker.applyWriteOperations(
        for (modification <- modifications)
          yield
            WriteOperation
              .Insert(pendingModificationsCollectionName, Scala2Js.toJsMap(ModificationWithId(modification))))
    }

  override def removePendingModifications(modifications: Seq[EntityModification]): Future[Unit] =
    serializingWriteQueue.schedule {
      webWorker.applyWriteOperations(
        for (modification <- modifications)
          yield
            WriteOperation
              .Remove(pendingModificationsCollectionName, Scala2Js.toJs(ModificationWithId(modification).id)))
    }

  override def setSingletonValue[V](key: SingletonKey[V], value: V) = serializingWriteQueue.schedule {
    implicit val converter = key.valueConverter
    webWorker.applyWriteOperations(
      Seq(
        WriteOperation.Remove(singletonsCollectionName, id = key.name),
        WriteOperation.Insert(
          singletonsCollectionName,
          Scala2Js.toJsMap(Singleton(key = key.name, value = Scala2Js.toJs(value))))
      ))
  }

  override def save(): Future[Unit] = serializingWriteQueue.schedule {
    webWorker.applyWriteOperations(Seq(WriteOperation.SaveDatabase))
  }

  override def resetAndInitialize(): Future[Unit] = serializingWriteQueue.schedule {
    async {
      console.log("  Resetting database...")
      await(
        webWorker.applyWriteOperations(
          Seq() ++
            (for (collectionName <- allCollectionNames)
              yield WriteOperation.RemoveCollection(collectionName)) ++
            (for (entityType <- EntityTypes.all)
              yield
                WriteOperation.AddCollection(
                  collectionNameOf(entityType),
                  uniqueIndices = Seq("id"),
                  indices = secondaryIndexFunction(entityType).map(_.name))) :+
            WriteOperation
              .AddCollection(singletonsCollectionName, uniqueIndices = Seq("id"), indices = Seq()) :+
            WriteOperation
              .AddCollection(pendingModificationsCollectionName, uniqueIndices = Seq("id"), indices = Seq())))
      console.log("  Resetting database done.")
    }
  }

  // **************** Private helper methods ****************//
  private def collectionNameOf(entityType: EntityType.any): String = s"entities_${entityType.name}"
  private val singletonsCollectionName = "singletons"
  private val pendingModificationsCollectionName = "pendingModifications"
  private def allCollectionNames: Seq[String] =
    EntityTypes.all.map(collectionNameOf) :+ singletonsCollectionName :+ pendingModificationsCollectionName
}

object LocalDatabaseImpl {

  def create(separateDbPerCollection: Boolean = false)(
      implicit webWorker: LocalDatabaseWebWorkerApi,
      secondaryIndexFunction: SecondaryIndexFunction,
  ): Future[LocalDatabase] = async {
    await(
      webWorker
        .create(dbName = "hydro-db", inMemory = false, separateDbPerCollection = separateDbPerCollection))
    new LocalDatabaseImpl()
  }

  def createStoredForTests(separateDbPerCollection: Boolean = false)(
      implicit webWorker: LocalDatabaseWebWorkerApi,
      secondaryIndexFunction: SecondaryIndexFunction,
  ): Future[LocalDatabase] = async {
    await(
      webWorker
        .create(dbName = "test-db", inMemory = false, separateDbPerCollection = separateDbPerCollection))
    new LocalDatabaseImpl()
  }

  def createInMemoryForTests(separateDbPerCollection: Boolean = false)(
      implicit webWorker: LocalDatabaseWebWorkerApi,
      secondaryIndexFunction: SecondaryIndexFunction,
  ): Future[LocalDatabase] =
    async {
      await(
        webWorker
          .create(dbName = "hydro-db", inMemory = true, separateDbPerCollection = separateDbPerCollection))
      new LocalDatabaseImpl()
    }

  case class SecondaryIndexFunction(function: EntityType.any => Seq[ModelField.any]) {
    def apply(entityType: EntityType.any): Seq[ModelField.any] = function(entityType)
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
}
