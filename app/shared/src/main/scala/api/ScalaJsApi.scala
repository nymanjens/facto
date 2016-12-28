package api

import models.accounting.config.Config
import models.manager.{Entity, EntityType}

import scala.collection.immutable.Seq

trait ScalaJsApi {

  def getAccountingConfig(): Config

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType.any]): Map[EntityType.any, Seq[Entity]]

  // TODO: Reomve insert/delete and accept Seq[EntityModification] instead
  def insertEntityWithId(entityType: EntityType.any, entity: Entity): Unit

  def deleteEntity(entityType: EntityType.any, entity: Entity): Unit
}
