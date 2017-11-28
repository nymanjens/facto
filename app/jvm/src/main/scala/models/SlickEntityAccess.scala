package models

import com.google.inject._
import models.accounting._
import models.manager.SlickEntityManager
import models.modification.EntityType.{
  BalanceCheckType,
  ExchangeRateMeasurementType,
  TransactionGroupType,
  TransactionType,
  UserType
}
import models.modification.{EntityType, SlickEntityModificationEntityManager}
import models.money.SlickExchangeRateMeasurementManager
import models.user.SlickUserManager

import scala.collection.immutable.Seq

final class SlickEntityAccess @Inject()(
    implicit override val userManager: SlickUserManager,
    override val balanceCheckManager: SlickBalanceCheckManager,
    override val transactionManager: SlickTransactionManager,
    override val transactionGroupManager: SlickTransactionGroupManager,
    override val exchangeRateMeasurementManager: SlickExchangeRateMeasurementManager,
    val entityModificationEntityManager: SlickEntityModificationEntityManager)
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
}
