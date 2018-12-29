package app.models.modification

import app.common.ScalaUtils
import hydro.models.Entity
import app.models.accounting._
import app.models.money.ExchangeRateMeasurement
import app.models.user.User

import scala.collection.immutable.Seq

/** Enumeration of all entity types that are transfered between server and client. */
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
  type any = EntityType[_ <: Entity]

  // @formatter:off
  implicit case object UserType extends EntityType[User] { override def entityClass = classOf[User]}
  implicit case object TransactionType extends EntityType[Transaction] { override def entityClass = classOf[Transaction] }
  implicit case object TransactionGroupType extends EntityType[TransactionGroup] { override def entityClass = classOf[TransactionGroup] }
  implicit case object BalanceCheckType extends EntityType[BalanceCheck] { override def entityClass = classOf[BalanceCheck] }
  implicit case object ExchangeRateMeasurementType extends EntityType[ExchangeRateMeasurement] { override def entityClass = classOf[ExchangeRateMeasurement] }
  // @formatter:on

  val values: Seq[EntityType.any] =
    Seq(UserType, TransactionType, TransactionGroupType, BalanceCheckType, ExchangeRateMeasurementType)
}
