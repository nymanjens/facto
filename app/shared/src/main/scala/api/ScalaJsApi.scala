package api

import java.nio.ByteBuffer

import api.ScalaJsApi.EntityType
import models.User
import models.accounting._
import models.accounting.config.Config
import models.accounting.money.ExchangeRateMeasurement
import models.manager.Entity

import scala.collection.immutable.Seq

trait ScalaJsApi {

  def getAccountingConfig(): Config

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType]): Map[EntityType, Seq[Entity]]

  def insertEntityWithId(entityType: EntityType, entity: Entity): Unit

  def removeEntity(entityType: EntityType, entityId: Long): Unit

  def test(entityType: EntityType)(entity: entityType.get): Unit = {

  }
}

object ScalaJsApi {

  sealed trait EntityType {
    type get <: Entity

    def checkRightType(entity: Entity): get = entity match {
      case e: get => e
    }
  }
  object EntityType {
    // @formatter:off
    object UserType extends EntityType { override type get = User }
    object TransactionType extends EntityType { override type get = Transaction }
    object TransactionGroupType extends EntityType { override type get = TransactionGroup }
    object BalanceCheckType extends EntityType { override type get = BalanceCheck }
    object ExchangeRateMeasurementType extends EntityType { override type get = ExchangeRateMeasurement }
    // @formatter:on
  }
}
