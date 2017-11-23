package models

import com.google.inject._
import models.accounting._
import models.manager.SlickEntityManager
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
}
