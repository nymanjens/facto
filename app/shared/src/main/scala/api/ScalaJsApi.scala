package api

import java.nio.ByteBuffer

import api.ScalaJsApi.EntityType
import common.ScalaUtils
import models.User
import models.accounting._
import models.accounting.config.Config
import models.accounting.money.ExchangeRateMeasurement
import models.manager.Entity

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

trait ScalaJsApi {

  def getAccountingConfig(): Config

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType]): Map[EntityType, Seq[Entity]]

  def insertEntityWithId(entityType: EntityType, entity: Entity): Unit

  def deleteEntity(entityType: EntityType, entity: Entity): Unit
}

object ScalaJsApi {

  sealed trait EntityType {
    type get <: Entity

    def entityClass: Class[get]

    def checkRightType(entity: Entity): get = {
      require(
        entity.getClass == entityClass,
        s"Got entity of type ${entity.getClass}, but this entityType requires $entityClass")
      entity.asInstanceOf[get]
    }

    def name: String = ScalaUtils.objectName(this)
    override def toString = name
  }
  object EntityType {
    // @formatter:off
    object UserType extends EntityType { override type get = User; override def entityClass = classOf[User]}
    object TransactionType extends EntityType { override type get = Transaction; override def entityClass = classOf[Transaction] }
    object TransactionGroupType extends EntityType { override type get = TransactionGroup; override def entityClass = classOf[TransactionGroup] }
    object BalanceCheckType extends EntityType { override type get = BalanceCheck; override def entityClass = classOf[BalanceCheck] }
    object ExchangeRateMeasurementType extends EntityType { override type get = ExchangeRateMeasurement; override def entityClass = classOf[ExchangeRateMeasurement] }
    // @formatter:on

    val values: Seq[EntityType] = Seq(UserType, TransactionType, TransactionGroupType, BalanceCheckType, ExchangeRateMeasurementType)
  }
}
