package api

import api.Picklers._
import api.ScalaJsApi.{GetAllEntitiesResponse, GetEntityModificationsResponse, UpdateToken}
import com.google.inject._
import common.time.Clock
import models.SlickEntityAccess
import models.accounting.config.Config
import models.manager.EntityType._
import models.manager._

import scala.collection.immutable.Seq

private[api] final class ScalaJsApiServer @Inject()(implicit accountingConfig: Config,
                                                    clock: Clock,
                                                    entityAccess: SlickEntityAccess) extends ScalaJsApi {

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

  override def getEntityModifications(updateToken: UpdateToken): GetEntityModificationsResponse = ???
  override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = ???

  // TODO: Remove
  //  override def insertEntityWithId(entityType: EntityType.any, entity: Entity): Unit = {
  //    require(entity.idOption.isDefined, s"Got an entity without ID ($entityType, $entity)")
  //
  //    // TODO: Add with ID instead of regular add
  //    getManager(entityType).add(entityType.checkRightType(entity))
  //  }
  //
  //  override def deleteEntity(entityType: EntityType.any, entity: Entity): Unit = {
  //    require(entity.idOption.isDefined, s"Got an entity without ID ($entityType, $entity)")
  //
  //    getManager(entityType).delete(entityType.checkRightType(entity))
  //  }

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
