package app.models.modification

import app.common.ScalaUtils
import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.money.ExchangeRateMeasurement
import hydro.models.Entity
import app.models.user.User

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

/** Enumeration of all entity types that are transfered between server and client. */
final class EntityType[E <: Entity](val entityClass: Class[E]) {
  type get = E

  def checkRightType(entity: Entity): get = {
    require(
      entity.getClass == entityClass,
      s"Got entity of type ${entity.getClass}, but this entityType requires $entityClass")
    entity.asInstanceOf[E]
  }

  lazy val name: String = entityClass.getSimpleName + "Type"
  override def toString = name
}
object EntityType {
  type any = EntityType[_ <: Entity]

  def apply[E <: Entity]()(implicit classTag: ClassTag[E]): EntityType[E] =
    new EntityType[E](classTag.runtimeClass.asInstanceOf[Class[E]])

  lazy val values: Seq[EntityType.any] =
    Seq(User.Type, Transaction.Type, TransactionGroup.Type, BalanceCheck.Type, ExchangeRateMeasurement.Type)
}
