package models.access

import api.ScalaJsApiClient
import common.LoggingUtils.logExceptions
import common.ScalaUtils.visibleForTesting
import jsfacades.LokiJs
import models.Entity
import models.access.SingletonKey._
import models.modification.EntityType
import models.modification.EntityModification

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala2js.Converters._

trait RemoteDatabaseProxy {

  // **************** Getters ****************//
  def newQuery[E <: Entity: EntityType](): LokiJs.ResultSet[E]

  /** Returns true if there are local pending `Add` modifications for the given entity. Note that only its id is used. */
  @Deprecated def hasLocalAddModifications[E <: Entity: EntityType](entity: E): Boolean

  // **************** Setters ****************//
  def persistModifications(modifications: Seq[EntityModification]): Future[Unit]
  final def persistModifications(modifications: EntityModification*): Future[Unit] =
    persistModifications(modifications.toVector)

  @Deprecated def clearLocalDatabase(): Future[Unit]

  // **************** Other ****************//
  def registerListener(listener: RemoteDatabaseProxy.Listener): Unit
  private[access] def startSchedulingModifiedEntityUpdates(): Unit
}

object RemoteDatabaseProxy {

  trait Listener {

    /**
      * Called when the local database was updated with a modification due to a local change request. This change is not
      * yet persisted in the remote database.
      */
    def addedLocally(modifications: Seq[EntityModification]): Unit

    def localModificationPersistedRemotely(modifications: Seq[EntityModification]): Unit = {}

    def addedRemotely(modifications: Seq[EntityModification]): Unit
  }
}
