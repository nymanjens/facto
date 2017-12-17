package models.accounting

import common.money.DatedMoney
import common.time.LocalDateTime
import models.accounting.config.{Config, MoneyReservoir}
import models.manager.EntityManager
import models.user.User
import models.{Entity, EntityAccess}

/** BalanceCheck entities are immutable. Just delete and create a new one when updating. */
case class BalanceCheck(issuerId: Long,
                        moneyReservoirCode: String,
                        balanceInCents: Long,
                        createdDate: LocalDateTime,
                        checkDate: LocalDateTime,
                        idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))

  override def toString = s"BalanceCheck(id=$idOption, issuer=$issuerId, $moneyReservoirCode)"

  def issuer(implicit entityAccess: EntityAccess): User = entityAccess.userManager.findByIdSync(issuerId)
  def moneyReservoir(implicit accountingConfig: Config): MoneyReservoir =
    accountingConfig.moneyReservoir(moneyReservoirCode)
  def balance(implicit accountingConfig: Config): DatedMoney =
    DatedMoney(balanceInCents, moneyReservoir.currency, checkDate)
}

object BalanceCheck {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[BalanceCheck]
}
