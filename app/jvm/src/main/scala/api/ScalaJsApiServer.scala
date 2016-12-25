package api

import api.ScalaJsApi.EntityType
import api.ScalaJsApi.EntityType._
import com.google.inject._
import models.{EntityAccess, SlickEntityAccess}
import models.accounting.config.Config
import models.manager.{Entity, EntityManager, SlickEntityManager}

import scala.collection.immutable.Seq

private[api] final class ScalaJsApiServer @Inject()(implicit accountingConfig: Config,
                                                    entityAccess: SlickEntityAccess) extends ScalaJsApi {

  override def getAccountingConfig(): Config = accountingConfig

  override def getAllEntities(types: Seq[EntityType]): Map[EntityType, Seq[Entity]] = {
    types
      .map(entityType => {
        entityType -> getManager(entityType).fetchAll()
      })
      .toMap
  }

  override def insertEntityWithId(entityType: EntityType, entity: Entity): Unit = {
    require(entity.idOption.isDefined, s"Got an entity without ID ($entityType, $entity)")

    // TODO: Add with ID instead of regular add
    getManager(entityType).add(entityType.checkRightType(entity))
  }

  override def deleteEntity(entityType: EntityType, entity: Entity): Unit = {
    require(entity.idOption.isDefined, s"Got an entity without ID ($entityType, $entity)")

    getManager(entityType).delete(entityType.checkRightType(entity))
  }

  private def getManager(entityType: EntityType): SlickEntityManager[entityType.get, _] = {
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
