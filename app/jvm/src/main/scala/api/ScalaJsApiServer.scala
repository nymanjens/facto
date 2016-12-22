package api

import api.ScalaJsApi.EntityType
import api.ScalaJsApi.EntityType._
import com.google.inject._
import models.EntityAccess
import models.accounting.config.Config
import models.manager.{Entity, EntityManager}

import scala.collection.immutable.Seq

private[api] final class ScalaJsApiServer @Inject()(implicit accountingConfig: Config,
                                                    entityAccess: EntityAccess) extends ScalaJsApi {

  override def getAccountingConfig(): Config = accountingConfig

  override def getAllEntities(types: Seq[EntityType]): Map[EntityType, Seq[Entity]] = {
    types
      .map(entityType => {
        entityType -> getManager(entityType).fetchAll()
      })
      .toMap
  }

  override def insertEntityWithId(entityType: EntityType, entity: Entity): Unit = {
    require(entity.idOption.isDefined, s"Gotten an entity without ID ($entityType, $entity)")

    // TODO: Add with ID instead of regular add
    getManager(entityType).add(entityType.checkRightType(entity))
  }

  override def removeEntity(entityType: EntityType, entityId: Long): Unit = {
    // TODO: Delete by ID
    // getManager(entityType).delete(entityId)
  }

  private def getManager(entityType: EntityType): EntityManager[entityType.get] = {
    val manager = entityType match {
      case UserType => entityAccess.userManager
      case TransactionType => entityAccess.transactionManager
      case TransactionGroupType => entityAccess.transactionGroupManager
      case BalanceCheckType => entityAccess.balanceCheckManager
      case ExchangeRateMeasurementType => entityAccess.exchangeRateMeasurementManager
    }
    manager.asInstanceOf[EntityManager[entityType.get]]
  }
}
