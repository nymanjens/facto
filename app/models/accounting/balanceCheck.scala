package models.accounting

import org.joda.time.DateTime

import common.Clock
import models.{User, Users}
import models.SlickUtils.dbApi._
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.manager.{EntityTable, ForwardingQueryableEntityManager, Identifiable, QueryableEntityManager}
import models.accounting.config.{Config, MoneyReservoir}

case class BalanceCheck(issuerId: Long,
                        moneyReservoirCode: String,
                        balance: Money,
                        createdDate: DateTime = Clock.now,
                        checkDate: DateTime,
                        idOption: Option[Long] = None) extends Identifiable[BalanceCheck] {

  override def withId(id: Long) = copy(idOption = Some(id))

  override def toString = s"BalanceCheck(issuer=$issuerId, $moneyReservoirCode, balance=$balance)"

  lazy val issuer: User = Users.findById(issuerId)

  lazy val moneyReservoir: MoneyReservoir = Config.moneyReservoir(moneyReservoirCode)
}

class BalanceChecks(tag: Tag) extends EntityTable[BalanceCheck](tag, BalanceChecks.tableName) {
  def issuerId = column[Long]("issuerId")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def balance = column[Money]("balance")
  def createdDate = column[DateTime]("createdDate")
  def checkDate = column[DateTime]("checkDate")

  override def * = (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <>(BalanceCheck.tupled, BalanceCheck.unapply)
}

object BalanceChecks {
  private val tableName: String = "BALANCE_CHECKS"
  val all = QueryableEntityManager.backedByDatabase[BalanceCheck, BalanceChecks](tag => new BalanceChecks(tag), tableName)
}
