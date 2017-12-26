package models.modificationhandler

import com.google.inject.Inject
import common.time.Clock
import models.access.{DbQuery, DbQueryExecutor}
import models.{Entity, SlickEntityAccess}
import models.manager.SlickEntityManager
import models.modification.EntityType.{
  BalanceCheckType,
  ExchangeRateMeasurementType,
  TransactionGroupType,
  TransactionType,
  UserType
}
import models.modification.{
  EntityModification,
  EntityModificationEntity,
  EntityType,
  SlickEntityModificationEntityManager
}
import models.user.User

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Handles storage and application of entity modifications.
  *
  * This class is the only point of entry for making changes to the database. Don't call the entity managers directly.
  */
final class EntityModificationHandler @Inject()(
    implicit clock: Clock,
    entityAccess: SlickEntityAccess,
    entityModificationManager: SlickEntityModificationEntityManager) {

  def persistEntityModifications(modifications: EntityModification*)(implicit user: User): Unit = {
    persistEntityModifications(modifications.toVector)
  }

  def persistEntityModifications(modifications: Seq[EntityModification])(implicit user: User): Unit = {
    for (modification <- modifications) {
      // Apply modification
      val entityType = modification.entityType
      modification match {
        case EntityModification.Add(entity) =>
          entityAccess.getManager(entityType).addIfNew(entity.asInstanceOf[entityType.get])
        case EntityModification.Update(entity) =>
          entityAccess.getManager(entityType).updateIfExists(entity.asInstanceOf[entityType.get])
        case EntityModification.Remove(entityId) =>
          entityAccess.getManager(entityType).deleteIfExists(entityId)
      }

      // Add modification
      entityModificationManager.addIfNew(
        EntityModificationEntity(
          idOption = Some(EntityModification.generateRandomId()),
          userId = user.id,
          modification = modification,
          date = clock.now
        ))

      updateTypeToAllEntities(modification)
    }
  }

  private val typeToAllEntities: Map[EntityType.any, mutable.Buffer[Entity]] = {
    for (entityType <- EntityType.values) yield {
      entityType -> mutable.Buffer(
        entityAccess.getManager(entityType).fetchAllSync().asInstanceOf[Seq[Entity]]: _*)
    }
  }.toMap

  def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]): Seq[E] = {
    implicit val entityType = dbQuery.entityType
    Await.result(
      DbQueryExecutor
        .fromEntities(typeToAllEntities(entityType).asInstanceOf[mutable.Buffer[E]])
        .data(dbQuery),
      Duration.Inf)
  }

  def executeCountQuery[E <: Entity](dbQuery: DbQuery[E]): Int = {
    implicit val entityType = dbQuery.entityType
    Await.result(
      DbQueryExecutor
        .fromEntities(typeToAllEntities(entityType).asInstanceOf[mutable.Buffer[E]])
        .count(dbQuery),
      Duration.Inf)
  }

  private def updateTypeToAllEntities(modification: EntityModification): Unit = {
    val entityType = modification.entityType
    entityType -> mutable.Buffer(
      entityAccess.getManager(entityType).fetchAllSync().asInstanceOf[Seq[Entity]]: _*)
  }
}
