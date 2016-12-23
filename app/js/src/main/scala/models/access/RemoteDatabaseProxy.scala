package models.access

import api.ScalaJsApi.EntityType

import scala.concurrent.Future
import api.ScalaJsApiClient
import jsfacades.Loki

import scala.collection.immutable.Seq

trait RemoteDatabaseProxy {

  // **************** Getters ****************//
  def newQuery(entityType: EntityType): Loki.ResultSet
  /** Returns true if there are local pending modifications for the given entity. Note that only its id is used. */
  def hasLocalModifications(entityType: EntityType)(entity: entityType.get): Boolean

  // **************** Setters ****************//
  def applyModifications(modifications: Seq[EntityModification]): Future[Unit]

  // **************** Other ****************//
  def registerListener(listener: RemoteDatabaseProxy.Listener): Unit
}

object RemoteDatabaseProxy {

  trait Listener {
    def addedLocally(entityModifications: Seq[EntityModification]): Unit
    def persistedRemotely(entityModifications: Seq[EntityModification]): Unit
  }
}
