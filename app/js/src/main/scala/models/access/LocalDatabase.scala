package models.access

import api.ScalaJsApi.EntityType
import jsfacades.Loki
import models.manager.Entity

import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala2js.Scala2Js
import scala2js.Converters._

// TODO: Move this to common
/**
  * Indicates an addition or removal of an immutable entity.
  *
  * This modification may used for desired modifications (not yet persisted) or to indicate an already changed state.
  */
sealed trait EntityModification {
  def entityType: EntityType
  def entityId: Long
}

object EntityModification {
  case class Add(override val entityType: EntityType, entity: Entity) extends EntityModification {
    require(entity.isInstanceOf[entityType.get])

    override def entityId: Long = entity.id
  }
  case class Remove(override val entityType: EntityType, override val entityId: Long) extends EntityModification
}

private[access] trait LocalDatabase {
  // **************** Getters ****************//
  def newQuery(entityType: EntityType): Loki.ResultSet
  def getSingletonValue[V](key: SingletonKey[V]): Option[V]
  def isEmpty(): Boolean

  // **************** Setters ****************//
  /** Applies given modification in memory but doesn't persist it in the browser's storage (call `save()` to do this). */
  def applyModifications(modifications: Seq[EntityModification]): Unit
  def addAll(entityType: EntityType)(entities: Seq[entityType.get]): Unit
  /** Sets given singleton value in memory but doesn't persist it in the browser's storage (call `save()` to do this). */
  def setSingletonValue[V](key: SingletonKey[V], value: V): Unit
  /** Persists all previously made changes to the browser's storage. */
  def save(): Future[Unit]
  def clear(): Future[Unit]
}

private[access] object LocalDatabase {

  def createLoadedFuture(): Future[LocalDatabase] = {
    val lokiDb: Loki.Database = Loki.Database.persistent("facto-db")
    lokiDb.loadDatabase() map (_ => new Impl(lokiDb))
  }

  private final class Impl(val lokiDb: Loki.Database) extends LocalDatabase {
    val entityCollections: Map[EntityType, Loki.Collection] = {
      for (entityType <- EntityType.values) yield {
        // TODO: Add primary indices
        entityType -> lokiDb.getOrAddCollection(s"entities_${entityType.name}")
      }
    }.toMap
    // TODO: Add primary index on key
    val singletonCollection = lokiDb.getOrAddCollection(s"singletons")

    // **************** Getters ****************//
    override def newQuery(entityType: EntityType) = entityCollections(entityType).chain()

    override def getSingletonValue[V](key: SingletonKey[V]): Option[V] = {
      implicit val converter = key.converter
      val value = singletonCollection.findOne("key" -> key.name)
      value.map(v => Scala2Js.toScala[V](v("value")))
    }

    override def isEmpty(): Boolean = {
      allCollections.toStream.filter(_.chain().count() != 0).isEmpty
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]): Unit = {
      ???
    }

    override def addAll(entityType: EntityType)(entities: Seq[entityType.get]): Unit = {
      implicit val converter = entityTypeToConverter(entityType)
      for (entity <- entities) {
        entityCollections(entityType).insert(Scala2Js.toJsMap(entity))
      }
    }

    override def setSingletonValue[V](key: SingletonKey[V], value: V): Unit = {
      implicit val converter = key.converter
      singletonCollection.findAndRemove("key" -> key.name)
      singletonCollection.insert(js.Dictionary(
        "key" -> key.name,
        "value" -> Scala2Js.toJs(value)
      ))
    }

    override def save(): Future[Unit] = {
      lokiDb.saveDatabase()
    }

    override def clear(): Future[Unit] = {
      for (collection <- allCollections) {
        collection.clear()
      }
      lokiDb.saveDatabase()
    }

    // **************** Private helper methods ****************//
    private def allCollections: Seq[Loki.Collection] = entityCollections.values.toList :+ singletonCollection
  }

  //
  //  def create(scalaJsApiClient: ScalaJsApiClient): LocalDatabase = {
  //    val loadingDatabase = new LoadingImpl()
  //    val forwardingDatabase = new ForwardingImpl(loadingDatabase)
  //
  //    val lokiDb: Loki.Database = Loki.Database.persistent("facto-db")
  //    lokiDb.loadDatabase() onSuccess { case _ =>
  //      // TODO: Load entities if empty (or failure, what happens?) (or do this in client of this method?)
  //      // TODO: reload everything if version mismatch (or do this in client of this method?)
  //      // TODO: Load updates from scalaJsApiClient? (or do this in client of this method?)
  //      val loadedDatabase = new LoadedImpl(lokiDb)
  //      forwardingDatabase.setDelegate(loadedDatabase)
  //    }
  //    forwardingDatabase
  //  }
  //
  //  private final class LoadingImpl extends LocalDatabase {
  //    val queuedOperations: mutable.Buffer[LocalDatabase => Unit] = mutable.Buffer()
  //
  //    override def newQuery(entityType: EntityType) = Loki.ResultSet.empty
  //
  //    override def applyModifications(modifications: Seq[EntityModification]) =
  //      appendMutableOperation(_.applyModifications(modifications))
  //    override def clear() =
  //      appendMutableOperation(_.clear())
  //
  //    private def appendMutableOperation[T](operation: LocalDatabase => Future[T]): Future[T] = {
  //      val promise = Promise[T]()
  //      queuedOperations.append(db => {
  //        val future = operation(db)
  //        future.onSuccess { case result => promise.success(result) }
  //        future.onFailure { case cause => promise.failure(cause) }
  //      })
  //      promise.future
  //    }
  //  }
  //
  //  private final class LoadedImpl(val lokiDb: Loki.Database) extends LocalDatabase {
  //    val entityCollections: Map[EntityType, Loki.Collection] = {
  //      for (entityType <- EntityType.values) yield {
  //        entityType -> lokiDb.getOrAddCollection(s"entities_${entityType.name}")
  //      }
  //    }.toMap
  //
  //    override def newQuery(entityType: EntityType) = entityCollections(entityType).chain()
  //
  //    override def applyModifications(modifications: Seq[EntityModification]) = ???
  //
  //    override def clear(): Future[Unit] = {
  //      for (collection <- entityCollections.values) {
  //        collection.clear()
  //      }
  //      lokiDb.saveDatabase()
  //    }
  //  }
  //
  //  private final class ForwardingImpl(var delegate: LocalDatabase) extends LocalDatabase {
  //    override def newQuery(entityType: EntityType) = delegate.newQuery(entityType)
  //    override def applyModifications(modifications: Seq[EntityModification]) = applyModifications(modifications)
  //    override def clear() = delegate.clear()
  //
  //    def setDelegate(newDeleagate: LocalDatabase): Unit = {
  //      delegate = newDeleagate
  //    }
  //  }
}
