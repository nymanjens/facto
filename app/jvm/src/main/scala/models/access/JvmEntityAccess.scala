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
import models.slick.{SlickEntityTableDef, SlickEntityManager}
import models.slick.SlickUtils.dbApi._
import models.slick.SlickUtils.dbRun
import models.user.User

import scala.collection.immutable.Seq
import scala.collection.mutable

final class JvmEntityAccess @Inject()(clock: Clock) extends EntityAccess {

  private val inMemoryEntityDatabase: InMemoryEntityDatabase = new InMemoryEntityDatabase(
    entitiesFetcher = new InMemoryEntityDatabase.EntitiesFetcher {
      override def fetch[E <: Entity](entityType: EntityType[E]): Seq[E] =
        getManager(entityType).fetchAll().asInstanceOf[Seq[E]]
    })

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType]() = DbResultSet.fromExecutor(queryExecutor[E].asAsync)
  override def newQuerySyncForUser() = newQuerySync[User]()
  def newQuerySync[E <: Entity: EntityType](): DbResultSet.Sync[E] =
    DbResultSet.fromExecutor(queryExecutor)

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = inMemoryEntityDatabase.queryExecutor

  def newSlickQuery[E <: Entity]()(
      implicit entityTableDef: SlickEntityTableDef[E]): TableQuery[entityTableDef.Table] =
    SlickEntityManager.forType.newQuery.asInstanceOf[TableQuery[entityTableDef.Table]]

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
      SlickEntityManager
        .forType[EntityModificationEntity]
        .addIfNew(
          EntityModificationEntity(
            idOption = Some(EntityModification.generateRandomId()),
            userId = user.id,
            modification = modification,
            date = clock.now
          ))

      inMemoryEntityDatabase.update(modification)
    }
  }

  // ********** Management methods ********** //
  def dropAndCreateTables(): Unit = {
    for (tableDef <- SlickEntityTableDef.all) {
      def internal[E <: Entity](tableDef: SlickEntityTableDef[E]) = {
        val entityManager = SlickEntityManager.forType[E](tableDef)
        dbRun(sqlu"""DROP TABLE IF EXISTS #${tableDef.tableName}""")
        entityManager.createTable()
      }
      internal(tableDef.asInstanceOf[SlickEntityTableDef[Entity]])
    }
  }

  // ********** Private helper methods ********** //
  private def getManager(entityType: EntityType.any): SlickEntityManager[entityType.get] =
    SlickEntityManager.forType(getEntityTableDef(entityType))

  private def getEntityTableDef(entityType: EntityType.any): SlickEntityTableDef[entityType.get] = {
    val tableDef = entityType match {
      case UserType                    => implicitly[SlickEntityTableDef[User]]
      case TransactionType             => implicitly[SlickEntityTableDef[Transaction]]
      case TransactionGroupType        => implicitly[SlickEntityTableDef[TransactionGroup]]
      case BalanceCheckType            => implicitly[SlickEntityTableDef[BalanceCheck]]
      case ExchangeRateMeasurementType => implicitly[SlickEntityTableDef[ExchangeRateMeasurement]]
    }
    tableDef.asInstanceOf[SlickEntityTableDef[entityType.get]]
  }
}
