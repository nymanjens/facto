package models

import com.google.common.hash.{HashCode, Hashing}
import common.Clock
import org.joda.time.DateTime
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.JodaToSqlDateMapper
import models.accounting.config.{Config, MoneyReservoir}
import models.accounting.money.{DatedMoney, Money}
import models._
import models.manager.{EntityTable, Entity, SlickEntityManager, ImmutableEntityManager}
import models.accounting.BalanceCheck

import SlickBalanceCheckManager.{BalanceChecks, tableName}

final class SlickBalanceCheckManager extends ImmutableEntityManager[BalanceCheck, BalanceChecks](
  SlickEntityManager.create[BalanceCheck, BalanceChecks](
    tag => new BalanceChecks(tag),
    tableName = tableName
  )) with BalanceCheck.Manager {
}

object SlickBalanceCheckManager {
  private val tableName: String = "BALANCE_CHECKS"

  final class BalanceChecks(tag: SlickTag) extends EntityTable[BalanceCheck](tag, tableName) {
    def issuerId = column[Long]("issuerId")
    def moneyReservoirCode = column[String]("moneyReservoirCode")
    def balance = column[Long]("balance")
    def createdDate = column[DateTime]("createdDate")
    def checkDate = column[DateTime]("checkDate")

    override def * = (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <> (BalanceCheck.tupled, BalanceCheck.unapply)
  }
}
