package models.access

import com.google.inject._
import common.time.Clock
import models.Entity
import models.accounting._
import models.modification.EntityType.{
  BalanceCheckType,
  ExchangeRateMeasurementType,
  TransactionGroupType,
  TransactionType,
  UserType
}
import models.modification.{EntityModification, EntityModificationEntity, EntityType}
import models.money.ExchangeRateMeasurement
import models.slick.SlickUtils.dbApi._
import models.slick.SlickUtils.dbRun
import models.slick.{SlickEntityManager, SlickEntityTableDef}
import models.user.User

import scala.collection.immutable.Seq
import scala.collection.mutable

private[access] final class InMemoryEntityDatabase(fetchEntitiesForType: EntityType.any => Seq[Entity]) {

  private val typeToAllEntities: mutable.Map[EntityType.any, Seq[Entity]] = mutable.Map({
    for (entityType <- EntityType.values) yield {
      entityType -> fetchEntitiesForType(entityType)
    }
  }: _*)

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = {
    val entityType = implicitly[EntityType[E]]
    DbQueryExecutor.fromEntities(typeToAllEntities(entityType).asInstanceOf[Seq[E]])
  }

  def update(modification: EntityModification): Unit = {
    val entityType = modification.entityType
    typeToAllEntities.put(entityType, fetchEntitiesForType(entityType))
  }
}
