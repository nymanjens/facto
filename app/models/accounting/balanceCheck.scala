package models.accounting

import org.joda.time.DateTime

import common.Clock
import models.{User, Users}
import models.ModelUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.accounting.config.Config
import models.activeslick._
import slick.driver.H2Driver.api._

case class BalanceCheck(issuerId: Long,
                        moneyReservoirCode: String,
                        balance: Money,
                        createdDate: DateTime = Clock.now,
                        checkDate: DateTime,
                        id: Option[Long] = None) extends Identifiable[BalanceCheck] {

  override def withId(id: Long) = copy(id = Some(id))

  override def toString = s"BalanceCheck(issuer=$issuerId, $moneyReservoirCode, balance=$balance)"

  lazy val issuer: User = Users.all.findById(issuerId)

  lazy val moneyReservoir = Config.moneyReservoirs(moneyReservoirCode)
}

class BalanceChecks(tag: Tag) extends EntityTable[BalanceCheck](tag, "BALANCE_CHECKS") {
  def issuerId = column[Long]("issuerId")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def balance = column[Money]("balance")
  def createdDate = column[DateTime]("createdDate")
  def checkDate = column[DateTime]("checkDate")

  def * = (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <>(BalanceCheck.tupled, BalanceCheck.unapply)
}

object BalanceChecks {
  val all = new EntityTableQuery[BalanceCheck, BalanceChecks](tag => new BalanceChecks(tag))
}
