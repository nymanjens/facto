package models.accounting

import collection.immutable.Seq

import org.joda.time.DateTime
import models.SlickUtils.dbApi._

import common.Clock
import common.ScalaUtils.objectName
import models.{User, Users}
import models.SlickUtils.{JodaToSqlDateMapper, dbRun}
import models.accounting.config.Config
import models.accounting.config.{Category, Account, MoneyReservoir}
import models.activeslick._

case class UpdateLog(userId: Long,
                     change: String,
                     date: DateTime = Clock.now,
                     id: Option[Long] = None) extends Identifiable[UpdateLog] {
  require(userId > 0)
  require(!change.isEmpty)
  for (idVal <- id) require(idVal > 0)

  override def withId(id: Long) = copy(id = Some(id))

  lazy val user: User = Users.all.findById(userId)
}

class UpdateLogs(tag: Tag) extends EntityTable[UpdateLog](tag, "UPDATE_LOGS") {
  def userId = column[Long]("userId")
  def change = column[String]("change")
  def date = column[DateTime]("date")

  override def * = (userId, change, date, id.?) <>(UpdateLog.tupled, UpdateLog.unapply)
}

object UpdateLogs {
  val all = new EntityTableQuery[UpdateLog, UpdateLogs](tag => new UpdateLogs(tag))

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEntries(n: Int): Seq[UpdateLog] =
    dbRun(all.sortBy(_.date.desc).take(n)).reverse.toList

  def addLog(user: User, operation: UpdateOperation, newOrDeletedValue: TransactionGroup): Unit = {
    require(newOrDeletedValue.id.isDefined, s"Given value must be persisted before logging it: ${newOrDeletedValue}")
    def fullyDescriptiveTransaction(t: Transaction): String = {
      s"Transaction(id=${t.id.get}, issuer=${t.issuer.loginName}, beneficiaryAccount=${t.beneficiaryAccountCode}, " +
        s"moneyReservoir=${t.moneyReservoirCode}, category=${t.categoryCode}, flow=${t.flow}, " +
        s"description=${t.description}, detailDescription=${t.detailDescription}, createdDate=${t.createdDate}, " +
        s"transactionDate=${t.transactionDate}, consumedDate=${t.consumedDate})"
    }
    def fullyDescriptiveString(group: TransactionGroup): String = {
      val transactionsString = group.transactions.map(fullyDescriptiveTransaction).mkString(", ")
      s"TransactionGroup(id=${group.id.get}, $transactionsString)"
    }
    addLog(user, operation, fullyDescriptiveString(newOrDeletedValue))
  }
  def addLog(user: User, operation: UpdateOperation, newOrDeletedValue: BalanceCheck): Unit = {
    require(newOrDeletedValue.id.isDefined, s"Given value must be persisted before logging it: ${newOrDeletedValue}")
    def fullyDescriptiveString(bc: BalanceCheck): String = {
      s"BalanceCheck(id=${bc.id.get}, issuer=${bc.issuer.loginName}, moneyReservoir=${bc.moneyReservoirCode}, " +
        s"balance=${bc.balance}, createdDate=${bc.createdDate}, checkDate=${bc.checkDate})"
    }
    addLog(user, operation, fullyDescriptiveString(newOrDeletedValue))
  }

  private def addLog(user: User, operation: UpdateOperation, newOrDeletedValueString: String): Unit = {
    val operationName = objectName(operation)
    val change = s"$operationName $newOrDeletedValueString"
    all.save(UpdateLog(user.id.get, change))
  }

  sealed trait UpdateOperation
  object AddNew extends UpdateOperation
  object Edit extends UpdateOperation
  object Delete extends UpdateOperation
}
