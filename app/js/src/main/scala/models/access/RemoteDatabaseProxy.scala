package models.access

import scala.async.Async.{async, await}
import scala.concurrent.duration._
import scala.scalajs.js
import api.ScalaJsApiClient
import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import common.ScalaUtils.visibleForTesting
import jsfacades.Loki
import models.access.SingletonKey._
import models.manager.{Entity, EntityModification, EntityType}

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

trait RemoteDatabaseProxy {

  // **************** Getters ****************//
  def newQuery[E <: Entity : EntityType](): Loki.ResultSet[E]
  /** Returns true if there are local pending `Add` modifications for the given entity. Note that only its id is used. */
  def hasLocalAddModifications[E <: Entity : EntityType](entity: E): Boolean

  // **************** Setters ****************//
  def persistModifications(modifications: Seq[EntityModification]): Future[Unit]
  def clearLocalDatabase(): Future[Unit]

  // **************** Other ****************//
  def registerListener(listener: RemoteDatabaseProxy.Listener): Unit
  private[access] def startSchedulingModifiedEntityUpdates(): Unit
}

object RemoteDatabaseProxy {

  private val localDatabaseAndEntityVersion = "1.0"

  private[access] def create(apiClient: ScalaJsApiClient,
                             possiblyEmptyLocalDatabase: LocalDatabase): Future[RemoteDatabaseProxy.Impl] = async {
    val db = possiblyEmptyLocalDatabase
    val populatedDb = {
      if (db.isEmpty() || !db.getSingletonValue(VersionKey).contains(localDatabaseAndEntityVersion)) {
        // Reset database
        await(db.clear())

        // Set version
        db.setSingletonValue(VersionKey, localDatabaseAndEntityVersion)

        // Add all entities
        val allEntitiesResponse = await(apiClient.getAllEntities(EntityType.values))
        for (entityType <- allEntitiesResponse.entityTypes) {
          def addAllToDb[E <: Entity](implicit entityType: EntityType[E]) =
            db.addAll(allEntitiesResponse.entities(entityType))
          addAllToDb(entityType)
        }
        db.setSingletonValue(NextUpdateTokenKey, allEntitiesResponse.nextUpdateToken)

        // Await because we don't want to save unpersisted modifications that can be made as soon as
        // the database becomes valid.
        await(db.save())
        db
      } else {
        db
      }
    }
    new Impl(apiClient, populatedDb)
  }

  trait Listener {
    /**
      * Called when the local database was updated with a modification due to a local change request. This change is not
      * yet persisted in the remote database.
      */
    def addedLocally(modifications: Seq[EntityModification]): Unit

    def localModificationPersistedRemotely(modifications: Seq[EntityModification]): Unit = {}

    def addedRemotely(modifications: Seq[EntityModification]): Unit
  }

  private[access] final class Impl(apiClient: ScalaJsApiClient,
                                   localDatabase: LocalDatabase) extends RemoteDatabaseProxy {

    private var listeners: Seq[Listener] = Seq()
    private val localAddModificationIds: Map[EntityType.any, mutable.Set[Long]] =
      EntityType.values.map(t => t -> mutable.Set[Long]()).toMap
    private var isCallingListeners: Boolean = false

    // **************** Getters ****************//
    override def newQuery[E <: Entity : EntityType](): Loki.ResultSet[E] = {
      localDatabase.newQuery[E]()
    }

    override def hasLocalAddModifications[E <: Entity : EntityType](entity: E): Boolean = {
      localAddModificationIds(implicitly[EntityType[E]]) contains entity.id
    }

    // **************** Setters ****************//
    override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = async {
      require(!isCallingListeners)

      localDatabase.applyModifications(modifications)

      for {
        modification <- modifications
        if modification.isInstanceOf[EntityModification.Add[_]]
      } localAddModificationIds(modification.entityType) += modification.entityId
      val listeners1 = invokeListenersAsync(_.addedLocally(modifications))

      await(apiClient.persistEntityModifications(modifications))

      for {
        modification <- modifications
        if modification.isInstanceOf[EntityModification.Add[_]]
      } localAddModificationIds(modification.entityType) -= modification.entityId
      val listeners2 = invokeListenersAsync(_.localModificationPersistedRemotely(modifications))

      await(listeners1)
      await(listeners2)
    }

    override def clearLocalDatabase(): Future[Unit] = async {
      require(!isCallingListeners)

      await(localDatabase.clear())
    }

    // **************** Other ****************//
    override def registerListener(listener: Listener): Unit = {
      require(!isCallingListeners)

      listeners = listeners :+ listener
    }

    override private[access] def startSchedulingModifiedEntityUpdates(): Unit = {
      var timeout = 5.seconds
      def cyclicLogic(): Unit = {
        updateModifiedEntities() onComplete { _ =>
          js.timers.setTimeout(timeout)(cyclicLogic)
          timeout * 1.02
        }
      }

      js.timers.setTimeout(0)(cyclicLogic)
    }

    // **************** Private helper methods ****************//
    private def invokeListenersAsync(func: Listener => Unit): Future[Unit] = {
      Future {
        logExceptions {
          require(!isCallingListeners)
          isCallingListeners = true
          listeners.foreach(func)
          isCallingListeners = false
        }
      }
    }

    @visibleForTesting private[access] def updateModifiedEntities(): Future[Unit] = async {
      val response = await(apiClient.getEntityModifications(localDatabase.getSingletonValue(NextUpdateTokenKey).get))
      if (response.modifications.nonEmpty) {
        localDatabase.applyModifications(response.modifications)
        localDatabase.setSingletonValue(NextUpdateTokenKey, response.nextUpdateToken)
        await(localDatabase.save())

        await(invokeListenersAsync(_.addedRemotely(response.modifications)))
      }
    }
  }
}
