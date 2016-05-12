package models.accounting

import org.joda.time.DateTime

import common.Clock
import models.{User, Users}
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.accounting.config.{Config, MoneyReservoir}
import models.activeslick._
import models.SlickUtils.dbApi._ 

case class BalanceCheck(issuerId: Long,
                        moneyReservoirCode: String,
                        balance: Money,
                        createdDate: DateTime = Clock.now,
                        checkDate: DateTime,
                        id: Option[Long] = None) extends Identifiable[BalanceCheck] {

  override def withId(id: Long) = copy(id = Some(id))

  override def toString = s"BalanceCheck(issuer=$issuerId, $moneyReservoirCode, balance=$balance)"

  lazy val issuer: User = Users.all.findById(issuerId)

  lazy val moneyReservoir: MoneyReservoir = Config.moneyReservoir(moneyReservoirCode)
}

class BalanceChecks(tag: Tag) extends EntityTable[BalanceCheck](tag, "BALANCE_CHECKS") {
  def issuerId = column[Long]("issuerId")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def balance = column[Money]("balance")
  def createdDate = column[DateTime]("createdDate")
  def checkDate = column[DateTime]("checkDate")

  override def * = (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <>(BalanceCheck.tupled, BalanceCheck.unapply)
}

object BalanceChecks {
  val all = new EntityTableQuery[BalanceCheck, BalanceChecks](tag => new BalanceChecks(tag))
}
