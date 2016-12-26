package api

import api.ScalaJsApi.EntityType
import common.ScalaUtils
import models.User
import models.accounting._
import models.accounting.config.Config
import models.accounting.money.ExchangeRateMeasurement
import models.manager.Entity

import scala.collection.immutable.Seq

trait ScalaJsApi {

  def getAccountingConfig(): Config

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType.Any]): Map[EntityType.Any, Seq[Entity]]

  // TODO: Reomve insert/delete and accept Seq[EntityModification] instead
  def insertEntityWithId(entityType: EntityType.Any, entity: Entity): Unit

  def deleteEntity(entityType: EntityType.Any, entity: Entity): Unit
}

object ScalaJsApi {

  sealed trait EntityType[E <: Entity] {
    type get = E

    def entityClass: Class[E]

    def checkRightType(entity: Entity): get = {
      require(
        entity.getClass == entityClass,
        s"Got entity of type ${entity.getClass}, but this entityType requires $entityClass")
      entity.asInstanceOf[E]
    }

    def name: String = ScalaUtils.objectName(this)
    override def toString = name
  }
  object EntityType {
    type Any = EntityType[_ <: Entity]

    // @formatter:off
    object UserType extends EntityType[User] { override def entityClass = classOf[User]}
    object TransactionType extends EntityType[Transaction] { override def entityClass = classOf[Transaction] }
    object TransactionGroupType extends EntityType[TransactionGroup] { override def entityClass = classOf[TransactionGroup] }
    object BalanceCheckType extends EntityType[BalanceCheck] { override def entityClass = classOf[BalanceCheck] }
    object ExchangeRateMeasurementType extends EntityType[ExchangeRateMeasurement] { override def entityClass = classOf[ExchangeRateMeasurement] }
    // @formatter:on

    val values: Seq[EntityType.Any] = Seq(UserType, TransactionType, TransactionGroupType, BalanceCheckType, ExchangeRateMeasurementType)
  }
}
