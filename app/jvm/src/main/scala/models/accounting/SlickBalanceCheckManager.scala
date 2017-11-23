package models

import common.time.LocalDateTime
import models.SlickBalanceCheckManager.{BalanceChecks, tableName}
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.accounting.BalanceCheck
import models.manager.{EntityTable, ImmutableEntityManager, SlickEntityManager}

final class SlickBalanceCheckManager
    extends ImmutableEntityManager[BalanceCheck, BalanceChecks](
      SlickEntityManager.create[BalanceCheck, BalanceChecks](
        tag => new BalanceChecks(tag),
        tableName = tableName
      ))
    with BalanceCheck.Manager

object SlickBalanceCheckManager {
  private val tableName: String = "BALANCE_CHECKS"

  final class BalanceChecks(tag: SlickTag) extends EntityTable[BalanceCheck](tag, tableName) {
    def issuerId = column[Long]("issuerId")
    def moneyReservoirCode = column[String]("moneyReservoirCode")
    def balance = column[Long]("balance")
    def createdDate = column[LocalDateTime]("createdDate")
    def checkDate = column[LocalDateTime]("checkDate")

    override def * =
      (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <> (BalanceCheck.tupled, BalanceCheck.unapply)
  }
}
