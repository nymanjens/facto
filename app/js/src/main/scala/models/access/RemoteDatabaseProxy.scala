package models.access

import api.ScalaJsApi.EntityType
import api.ScalaJsApiClient
import jsfacades.Loki
import jsfacades.Loki.ResultSet
import models.access.SingletonKey._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

trait RemoteDatabaseProxy {

  // **************** Getters ****************//
  def newQuery(entityType: EntityType): Loki.ResultSet
  /** Returns true if there are local pending modifications for the given entity. Note that only its id is used. */
  def hasLocalModifications(entityType: EntityType)(entity: entityType.get): Boolean

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
    def addedLocally(entityModifications: Seq[EntityModification]): Unit
    def persistedRemotely(entityModifications: Seq[EntityModification]): Unit
  }

  private[access] final class Impl(apiClient: ScalaJsApiClient,
                                   localDatabase: Future[LocalDatabase]) extends RemoteDatabaseProxy {
    // TODO: Start getting latest changes, starting at t0 (db.setSingletonValue(LastUpdateTimeKey, ...))

    // **************** Getters ****************//
    override def newQuery(entityType: EntityType): ResultSet = ???
    /** Returns true if there are local pending modifications for the given entity. Note that only its id is used. */
    override def hasLocalModifications(entityType: EntityType)(entity: entityType.get): Boolean = ???

    // **************** Setters ****************//
    override def persistModifications(modifications: Seq[EntityModification]): Unit = ???
    override def clearLocalDatabase(): Future[Unit] = localDatabase.flatMap(_.clear())

    // **************** Other ****************//
    override def registerListener(listener: Listener): Unit = ???
  }
}
