package models.modificationhandler

import com.google.inject.Inject
import common.time.Clock
import models.SlickEntityAccess
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

/**
  * Handles storage and application of entity modifications.
  *
  * This class is the only point of entry for making changes to the database. Don't call the entity managers directly.
  */
final class EntityModificationHandler @Inject()(
    implicit clock: Clock,
    entityAccess: SlickEntityAccess,
    entityModificationManager: SlickEntityModificationEntityManager) {

  def persistEntityModifications(modifications: Seq[EntityModification])(implicit user: User): Unit = {
    for (modification <- modifications) {
      // Apply modification
      val entityType = modification.entityType
      modification match {
        case EntityModification.Add(entity) =>
          getManager(entityType).addIfNew(entity.asInstanceOf[entityType.get])
        case EntityModification.Update(entity) =>
          getManager(entityType).updateIfExists(entity.asInstanceOf[entityType.get])
        case EntityModification.Remove(entityId) =>
          getManager(entityType).deleteIfExists(entityId)
      }

      // Add modification
      entityModificationManager.add(
        EntityModificationEntity(
          userId = user.id,
          modification = modification,
          date = clock.now
        ))
    }
  }

  private def getManager(entityType: EntityType.any): SlickEntityManager[entityType.get, _] = {
    val manager = entityType match {
      case UserType => entityAccess.userManager
      case TransactionType => entityAccess.transactionManager
      case TransactionGroupType => entityAccess.transactionGroupManager
      case BalanceCheckType => entityAccess.balanceCheckManager
      case ExchangeRateMeasurementType => entityAccess.exchangeRateMeasurementManager
    }
    manager.asInstanceOf[SlickEntityManager[entityType.get, _]]
  }
}
