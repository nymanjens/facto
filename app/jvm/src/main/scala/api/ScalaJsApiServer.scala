package api

import api.Picklers._
import api.ScalaJsApi.{GetAllEntitiesResponse, GetEntityModificationsResponse, UpdateToken}
import com.google.inject._
import common.time.Clock
import models.SlickUtils.dbApi._
import models.SlickUtils.{dbRun, localDateTimeToSqlDateMapper}
import models.accounting.{SlickUpdateLogManager, UpdateLog}
import models.accounting.config.Config
import models.manager.EntityType._
import models.manager._
import models.{EntityModificationEntity, SlickEntityAccess, SlickEntityModificationEntityManager, User}

import scala.collection.immutable.Seq

private[api] final class ScalaJsApiServer @Inject()(implicit accountingConfig: Config,
                                                    clock: Clock,
                                                    entityAccess: SlickEntityAccess,
                                                    updateLogManager: SlickUpdateLogManager,
                                                    entityModificationManager: SlickEntityModificationEntityManager) extends ScalaJsApi {

  override def getAccountingConfig(): Config = accountingConfig

  override def getAllEntities(types: Seq[EntityType.any]) = {
    // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
    val nextUpdateToken: UpdateToken = clock.now
    val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
      types
        .map(entityType => {
          entityType -> getManager(entityType).fetchAll()
        })
        .toMap
    }

    GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
  }

  override def getEntityModifications(updateToken: UpdateToken): GetEntityModificationsResponse = {
    // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
    val nextUpdateToken: UpdateToken = clock.now

    val modifications = {
      val modificationEntities = dbRun(entityModificationManager.newQuery
        .filter(_.date >= nextUpdateToken)
        .sortBy(_.date))
      modificationEntities.toStream.map(_.modification).toVector
    }

    GetEntityModificationsResponse(modifications, nextUpdateToken)
  }

  override def persistEntityModifications(modifications: Seq[EntityModification])(implicit user: User): Unit = {
    for (modification <- modifications) {
      // Apply modification
      val entityType = modification.entityType
      modification match {
        case EntityModification.Add(entity) =>
          getManager(entityType).addWithId(entity.asInstanceOf[entityType.get])
        case EntityModification.Remove(entityId) =>
          getManager(entityType).delete(getManager(entityType).findById(entityId))
      }

      // Add modification
      entityModificationManager.add(EntityModificationEntity(
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
