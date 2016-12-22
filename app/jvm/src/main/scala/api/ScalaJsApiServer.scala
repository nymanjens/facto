package api

import java.nio.ByteBuffer

import api.ScalaJsApi.EntityType
import api.ScalaJsApi.EntityType._
import boopickle.Default._
import api.Picklers._
import com.google.inject._
import models.{EntityAccess, User}
import models.accounting._
import models.accounting.config.Config
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{Entity, EntityManager}

import scala.collection.immutable.Seq

private[api] final class ScalaJsApiServer @Inject()(implicit accountingConfig: Config,
                                                    entityAccess: EntityAccess) extends ScalaJsApi {

  override def getAccountingConfig(): Config = accountingConfig

  override def getAllEntities(types: Seq[EntityType]): Map[EntityType, Seq[ByteBuffer]] = {
    types
      .map(entityType => {
        val entities = getManager(entityType)
          .fetchAll()
          .map(e => pickleEntity(entityType, e.asInstanceOf[Entity]))
        entityType -> entities
      })
      .toMap
  }

  override def insertEntityWithId(entityType: EntityType, entityBytes: ByteBuffer): Unit = {
    val entity = unpickleEntity(entityType, entityBytes)
    require(entity.idOption.isDefined, s"Gotten an entity without ID ($entityType, $entity)")

    def doInsert[E <: Entity](entity: Entity) = {
      // TODO: Add with ID instead of regular add
      getManager(entityType).asInstanceOf[EntityManager[E]].add(entity.asInstanceOf[E])
    }
    doInsert(entity)
  }

  override def removeEntity(entityType: EntityType, entityId: Long): Unit = {
    // TODO: Delete by ID
    // getManager(entityType).delete(entityId)
  }

  private def getManager(entityType: EntityType): EntityManager[_] =
    entityType match {
      case UserType => entityAccess.userManager
      case TransactionType => entityAccess.transactionManager
      case TransactionGroupType => entityAccess.transactionGroupManager
      case BalanceCheckType => entityAccess.balanceCheckManager
      case ExchangeRateMeasurementType => entityAccess.exchangeRateMeasurementManager
    }
}
