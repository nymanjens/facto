package models

import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import com.google.inject._
import common.time.Clock
import models.access.{DbQueryExecutor, DbResultSet}
import models.accounting._
import models.manager.SlickEntityManager
import models.modification.EntityType.{
  BalanceCheckType,
  ExchangeRateMeasurementType,
  TransactionGroupType,
  TransactionType,
  UserType
}
import models.modification.{
  EntityModification,
  EntityModificationEntity,
  EntityType,
  SlickEntityModificationEntityManager
}
import models.money.SlickExchangeRateMeasurementManager
import models.user.{SlickUserManager, User}

import scala.collection.immutable.Seq
import scala.collection.mutable

final class SlickEntityAccess @Inject()(
    implicit override val userManager: SlickUserManager,
    override val balanceCheckManager: SlickBalanceCheckManager,
    override val transactionManager: SlickTransactionManager,
    override val transactionGroupManager: SlickTransactionGroupManager,
    override val exchangeRateMeasurementManager: SlickExchangeRateMeasurementManager,
    val entityModificationEntityManager: SlickEntityModificationEntityManager,
    clock: Clock)
    extends EntityAccess {

  val allEntityManagers: Seq[SlickEntityManager[_, _]] =
    Seq(
      userManager,
      transactionManager,
      transactionGroupManager,
      balanceCheckManager,
      exchangeRateMeasurementManager,
      entityModificationEntityManager
    )

  def getManager(entityType: EntityType.any): SlickEntityManager[entityType.get, _] = {
    val manager = entityType match {
      case UserType => userManager
      case TransactionType => transactionManager
      case TransactionGroupType => transactionGroupManager
      case BalanceCheckType => balanceCheckManager
      case ExchangeRateMeasurementType => exchangeRateMeasurementManager
    }
    manager.asInstanceOf[SlickEntityManager[entityType.get, _]]
  }

  // **************** Getters ****************//
  private val typeToAllEntities: mutable.Map[EntityType.any, Seq[Entity]] = mutable.Map({
    for (entityType <- EntityType.values) yield {
      entityType -> getManager(entityType).fetchAllSync()
    }
  }: _*)

  override def newQuery[E <: Entity: EntityType]() = DbResultSet.fromExecutor(newQueryExecutor)

  def newQueryExecutor[E <: Entity: EntityType]: DbQueryExecutor[E] = {
    val entityType = implicitly[EntityType[E]]
    DbQueryExecutor.fromEntities(typeToAllEntities(entityType).asInstanceOf[Seq[E]])
  }

  private def updateTypeToAllEntities(modification: EntityModification): Unit = {
    val entityType = modification.entityType
    typeToAllEntities.put(entityType, getManager(entityType).fetchAllSync())
  }

  // **************** Setters ****************//
  def persistEntityModifications(modifications: EntityModification*)(implicit user: User): Unit = {
    persistEntityModifications(modifications.toVector)
  }

  def persistEntityModifications(modifications: Seq[EntityModification])(implicit user: User): Unit = {
    for (modification <- modifications) {
      // Apply modification
      val entityType = modification.entityType
      modification match {
        case EntityModification.Add(entity) =>
          getManager(entityType).addIfNew(entity.asInstanceOf[entityType.get])
        case EntityModification.Update(entity) =>
          getManager(entityType).updateIfExists(entity.asInstanceOf[entityType.get])
        case EntityModification.Remove(entityId) =>
          getManager(entityType).deleteIfExists(entityId)
      }

      // Add modification
      entityModificationEntityManager.addIfNew(
        EntityModificationEntity(
          idOption = Some(EntityModification.generateRandomId()),
          userId = user.id,
          modification = modification,
          date = clock.now
        ))

      updateTypeToAllEntities(modification)
    }
  }

  // ********** Management methods ********** //
  def dropAndCreateTables(): Unit = {
    for (entityManager <- allEntityManagers) {
      dbRun(sqlu"""DROP TABLE IF EXISTS #${entityManager.tableName}""")
      entityManager.createTable()
    }
  }
}
