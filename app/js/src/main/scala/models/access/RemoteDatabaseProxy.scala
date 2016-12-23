package models.access

import api.ScalaJsApi.EntityType
import api.ScalaJsApiClient
import jsfacades.Loki
import jsfacades.Loki.ResultSet
import models.access.SingletonKey._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Success

trait RemoteDatabaseProxy {

  // **************** Getters ****************//
  def newQuery(entityType: EntityType): Loki.ResultSet
  /** Returns true if there are local pending `Add` modifications for the given entity. Note that only its id is used. */
  def hasLocalAddModifications(entityType: EntityType)(entity: entityType.get): Boolean

  // **************** Setters ****************//
  def persistModifications(modifications: Seq[EntityModification]): Unit
  def clearLocalDatabase(): Future[Unit]

  // **************** Other ****************//
  def registerListener(listener: RemoteDatabaseProxy.Listener): Unit
}

object RemoteDatabaseProxy {

  val localDatabaseAndEntityVersion = "1.0"

  def create(apiClient: ScalaJsApiClient): RemoteDatabaseProxy = {
    val futureDb = LocalDatabase.createLoadedFuture()
    val validDb = futureDb.flatMap(db => {
      if (db.isEmpty() || db.getSingletonValue(VersionKey) != Some(localDatabaseAndEntityVersion)) {
        // Reset database
        db.clear()
        db.setSingletonValue(VersionKey, localDatabaseAndEntityVersion)
        // TODO: db.setSingletonValue(LastUpdateTimeKey, ...)
        apiClient.getAllEntities(EntityType.values).map(resultMap => {
          for ((entityType, entities) <- resultMap) {
            db.addAll(entityType)(entities.asInstanceOf[Seq[entityType.get]])
          }
          db.save() // don't wait for this because it doesn't really matter when this completes
          db
        })
      } else {
        Future.successful(db)
      }
    })
    new Impl(apiClient, validDb)
  }

  trait Listener {
    /**
      * Called when the local database was updated with a modification due to a local change request. This change is not
      * yet persisted in the remote database.
      */
    def addedLocally(entityModifications: Seq[EntityModification]): Unit

    /**
      * Called when a remote entity was changed, either due to a change request from this or another client.
      *
      * Note that a preceding `addedLocally()` call may have been made earlier for the same modifications, but this is
      * not always the case.
      */
    def persistedRemotely(entityModifications: Seq[EntityModification]): Unit
    /**
      * Called after the initial loading or reloading of the database. This also gets called when the database is
      * cleared.
      */
    def loadedDatabase(): Unit
  }

  private[access] final class Impl(apiClient: ScalaJsApiClient,
                                   localDatabase: Future[LocalDatabase]) extends RemoteDatabaseProxy {

    val listeners: mutable.Buffer[Listener] = mutable.Buffer()
    val localAddModificationIds: Map[EntityType, mutable.Set[Long]] =
      EntityType.values.map(t => t -> mutable.Set[Long]()).toMap

    localDatabase.onSuccess { case db => listeners.foreach(_.loadedDatabase()) }
    // TODO: Start getting latest changes, starting at t0 (db.setSingletonValue(LastUpdateTimeKey, ...))

    // **************** Getters ****************//
    override def newQuery(entityType: EntityType): Loki.ResultSet = {
      localDatabase.value map { case Success(db) => db.newQuery(entityType) } getOrElse Loki.ResultSet.empty
    }
    /** Returns true if there are local pending modifications for the given entity. Note that only its id is used. */
    override def hasLocalAddModifications(entityType: EntityType)(entity: entityType.get): Boolean = {
      localAddModificationIds(entityType) contains entity.id
    }

    // **************** Setters ****************//
    override def persistModifications(modifications: Seq[EntityModification]): Unit = {
      localDatabase onSuccess { case db =>
        db.applyModifications(modifications)
        listeners.foreach(_.addedLocally(modifications))
        for {
          modification <- modifications
          if modification.isInstanceOf[EntityModification.Add]
        } localAddModificationIds(modification.entityType) += modification.entityId

        // TODO: persist remotely and execute code below:
        //        for {
        //          modification <- modifications
        //          if modification.isInstanceOf[EntityModification.Add]
        //        } localAddModificationIds(modification.entityType) -= modification.entityId
        //        listeners.foreach(_.persistedRemotely(modifications))
      }
    }
    override def clearLocalDatabase(): Future[Unit] = {
      val resultFuture = localDatabase.flatMap(_.clear())
      resultFuture onSuccess { case _ => listeners.foreach(_.loadedDatabase()) }
      resultFuture
    }

    // **************** Other ****************//
    override def registerListener(listener: Listener): Unit = {
      listeners += listener
    }
  }
}
