package models.accounting

import com.google.common.hash.{HashCode, Hashing}
import common.Clock
import common.cache.UniquelyHashable
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.accounting.config.{Config, MoneyReservoir}
import models.manager.{Entity, EntityManager, EntityTable, ImmutableEntityManager}
import models.{User, Users}
import org.joda.time.DateTime

/** BalanceCheck entities are immutable. Just delete and create a new one when updating. */
case class BalanceCheck(issuerId: Long,
                        moneyReservoirCode: String,
                        balance: Money,
                        createdDate: DateTime = Clock.now,
                        checkDate: DateTime,
                        idOption: Option[Long] = None) extends Entity[BalanceCheck] with UniquelyHashable {

  override def withId(id: Long) = copy(idOption = Some(id))

  override def toString = s"BalanceCheck(issuer=$issuerId, $moneyReservoirCode, balance=$balance)"

  lazy val issuer: User = Users.findById(issuerId)

  lazy val moneyReservoir: MoneyReservoir = Config.moneyReservoir(moneyReservoirCode)

  // BalanceChecks are immutable
  override def uniqueHash = HashCode.fromLong(id)
}

class BalanceChecks(tag: SlickTag) extends EntityTable[BalanceCheck](tag, BalanceChecks.tableName) {
  def issuerId = column[Long]("issuerId")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def balance = column[Money]("balance")
  def createdDate = column[DateTime]("createdDate")
  def checkDate = column[DateTime]("checkDate")

  override def * = (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <>(BalanceCheck.tupled, BalanceCheck.unapply)
}

object BalanceChecks extends ImmutableEntityManager[BalanceCheck, BalanceChecks](
  EntityManager.create[BalanceCheck, BalanceChecks](
    tag => new BalanceChecks(tag), tableName = "BALANCE_CHECKS"))
