package models.access

import api.ScalaJsApi.EntityType
import api.ScalaJsApiClient
import jsfacades.Loki
import models.access.SingletonKey._
import models.manager.Entity

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
  def persistModifications(modifications: Seq[EntityModification]): Unit
  def clearLocalDatabase(): Future[Unit]

  // **************** Other ****************//
  def registerListener(listener: RemoteDatabaseProxy.Listener): Unit
}

object RemoteDatabaseProxy {

  private val localDatabaseAndEntityVersion = "1.0"

  trait Listener {
    /**
      * Called when the local database was updated with a modification due to a local change request. This change is not
      * yet persisted in the remote database.
      */
    def addedLocally(modifications: Seq[EntityModification]): Unit

    /**
      * Called when a remote entity was changed, either due to a change request from this or another client.
      *
      * Note that a preceding `addedLocally()` call may have been made earlier for the same modifications, but this is
      * not always the case.
      */
    def persistedRemotely(modifications: Seq[EntityModification]): Unit
    /**
      * Called after the initial loading or reloading of the database. This also gets called when the database is
      * cleared.
      */
    def loadedDatabase(): Unit
  }

  private[access] final class Impl(apiClient: ScalaJsApiClient,
                                   possiblyEmptyLocalDatabase: Future[LocalDatabase]) extends RemoteDatabaseProxy {

    private val localDatabase: Future[LocalDatabase] = possiblyEmptyLocalDatabase flatMap toValidDatabase
    private var listeners: Seq[Listener] = Seq()
    private val localAddModificationIds: Map[EntityType.any, mutable.Set[Long]] =
      EntityType.values.map(t => t -> mutable.Set[Long]()).toMap
    private var isCallingListeners: Boolean = false

    localDatabase.onSuccess { case db => invokeListenersAsync(_.loadedDatabase()) }
    // TODO: Start getting latest changes, starting at t0 (db.setSingletonValue(LastUpdateTimeKey, ...))

    // **************** Getters ****************//
    override def newQuery[E <: Entity : EntityType](): Loki.ResultSet[E] = {
      val maybeDb = localDatabase.value.flatMap(_.toOption)
      maybeDb.map(_.newQuery[E]()) getOrElse Loki.ResultSet.empty[E]
    }
    override def hasLocalAddModifications[E <: Entity : EntityType](entity: E): Boolean = {
      localAddModificationIds(implicitly[EntityType[E]]) contains entity.id
    }

    // **************** Setters ****************//
    override def persistModifications(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)

      localDatabase onSuccess { case db =>
        db.applyModifications(modifications)
        invokeListenersAsync(_.addedLocally(modifications))
        for {
          modification <- modifications
          if modification.isInstanceOf[EntityModification.Add[_]]
        } localAddModificationIds(modification.entityType) += modification.entityId

        // TODO: persist remotely and execute code below:
        //        for {
        //          modification <- modifications
        //          if modification.isInstanceOf[EntityModification.Add]
        //        } localAddModificationIds(modification.entityType) -= modification.entityId
        //        invokeListeners(_.persistedRemotely(modifications))
      }
    }
    override def clearLocalDatabase(): Future[Unit] = {
      require(!isCallingListeners)

      val resultFuture = localDatabase.flatMap(_.clear())
      resultFuture onSuccess { case _ => invokeListenersAsync(_.loadedDatabase()) }
      resultFuture
    }

    // **************** Other ****************//
    override def registerListener(listener: Listener): Unit = {
      require(!isCallingListeners)

      listeners = listeners :+ listener
    }

    // **************** Private helper methods ****************//
    private def invokeListenersAsync(func: Listener => Unit): Unit = {
      Future {
        require(!isCallingListeners)
        isCallingListeners = true
        listeners.foreach(func)
        isCallingListeners = false
      }
    }

    private def toValidDatabase(db: LocalDatabase): Future[LocalDatabase] = {
      if (db.isEmpty() || !db.getSingletonValue(VersionKey).contains(localDatabaseAndEntityVersion)) {
        // Reset database
        db.clear()
        db.setSingletonValue(VersionKey, localDatabaseAndEntityVersion)
        // TODO: db.setSingletonValue(LastUpdateTimeKey, ...)
        apiClient.getAllEntities(EntityType.values).map(resultMap => {
          def addAllToDb[E <: Entity](entityType: EntityType[E])(entities: Seq[entityType.get]) =
            db.addAll(entities.asInstanceOf[Seq[E]])(entityType)
          for ((entityType, entities) <- resultMap) {
            addAllToDb(entityType)(entities.asInstanceOf[Seq[entityType.get]])
          }
          db.save() // don't wait for this because it doesn't really matter when this completes
          db
        })
      } else {
        Future.successful(db)
      }
    }
  }
}
