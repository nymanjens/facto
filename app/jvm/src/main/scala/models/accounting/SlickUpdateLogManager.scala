package models.accounting

import collection.immutable.Seq

import com.google.inject._
import common.time.LocalDateTime

import common.time.Clock
import common.ScalaUtils.objectName
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models._
import models.SlickUtils.{localDateTimeToSqlDateMapper, dbRun}
import models.manager.{EntityTable, ImmutableEntityManager, Entity, SlickEntityManager}
import models.accounting.config.Config
import models.accounting.config.{Category, Account, MoneyReservoir}

import UpdateLog.UpdateOperation
import SlickUpdateLogManager.{UpdateLogs, tableName}

final class SlickUpdateLogManager @Inject()(implicit accountingConfig: Config, clock: Clock)
    extends ImmutableEntityManager[UpdateLog, UpdateLogs](
      SlickEntityManager.create[UpdateLog, UpdateLogs](
        tag => new UpdateLogs(tag),
        tableName = tableName
      ))
    with UpdateLog.Manager {

  override def fetchLastNEntries(n: Int): Seq[UpdateLog] = {
    dbRun(newQuery.sortBy(_.date.desc).take(n)).reverse.toList
  }

  override def addLog(user: User, operation: UpdateOperation, newOrDeletedValue: TransactionGroup)(
      implicit entityAccess: EntityAccess): Unit = {
    require(
      newOrDeletedValue.idOption.isDefined,
      s"Given value must be persisted before logging it: ${newOrDeletedValue}")
    def fullyDescriptiveTransaction(t: Transaction): String = {
      s"Transaction(id=${t.id}, issuer=${t.issuer.loginName}, beneficiaryAccount=${t.beneficiaryAccountCode}, " +
        s"moneyReservoir=${t.moneyReservoirCode}, category=${t.categoryCode}, flow=${t.flow}, " +
        s"description=${t.description}, detailDescription=${t.detailDescription}, createdDate=${t.createdDate}, " +
        s"transactionDate=${t.transactionDate}, consumedDate=${t.consumedDate})"
    }
    def fullyDescriptiveString(group: TransactionGroup): String = {
      val transactionsString = group.transactions.map(fullyDescriptiveTransaction).mkString(", ")
      s"TransactionGroup(id=${group.id}, $transactionsString)"
    }
    addLog(user, operation, fullyDescriptiveString(newOrDeletedValue))
  }

  override def addLog(user: User, operation: UpdateOperation, newOrDeletedValue: BalanceCheck)(
      implicit entityAccess: EntityAccess): Unit = {
    require(
      newOrDeletedValue.idOption.isDefined,
      s"Given value must be persisted before logging it: ${newOrDeletedValue}")
    def fullyDescriptiveString(bc: BalanceCheck): String = {
      s"BalanceCheck(id=${bc.id}, issuer=${bc.issuer.loginName}, moneyReservoir=${bc.moneyReservoirCode}, " +
        s"balance=${bc.balance}, createdDate=${bc.createdDate}, checkDate=${bc.checkDate})"
    }
    addLog(user, operation, fullyDescriptiveString(newOrDeletedValue))
  }

  private def addLog(user: User, operation: UpdateOperation, newOrDeletedValueString: String): Unit = {
    val operationName = objectName(operation)
    val change = s"$operationName $newOrDeletedValueString"
    add(UpdateLog(user.id, change, date = clock.now))
  }
}
object SlickUpdateLogManager {
  private val tableName: String = "UPDATE_LOGS"

  final class UpdateLogs(tag: SlickTag) extends EntityTable[UpdateLog](tag, tableName) {
    def userId = column[Long]("userId")
    def change = column[String]("change")
    def date = column[LocalDateTime]("date")

    override def * = (userId, change, date, id.?) <> (UpdateLog.tupled, UpdateLog.unapply)
  }
}
