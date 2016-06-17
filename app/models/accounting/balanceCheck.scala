package models.accounting

import com.google.common.hash.Hashing
import common.Clock
import common.cache.UniquelyHashable
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.SlickUtils.{JodaToSqlDateMapper, MoneyToLongMapper}
import models.accounting.config.{Config, MoneyReservoir}
import models.manager.{Entity, EntityManager, EntityTable, ForwardingEntityManager}
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


  override def uniqueHash = Hashing.sha1().newHasher()
    .putLong(id)
    .putLong(balance.cents)
    .putLong(checkDate.getMillis)
    // Heuristic: If any other fields change (normally not the case), the hashCode will most likely change as well.
    .putInt(hashCode())
    .hash()
}

class BalanceChecks(tag: SlickTag) extends EntityTable[BalanceCheck](tag, BalanceChecks.tableName) {
  def issuerId = column[Long]("issuerId")
  def moneyReservoirCode = column[String]("moneyReservoirCode")
  def balance = column[Money]("balance")
  def createdDate = column[DateTime]("createdDate")
  def checkDate = column[DateTime]("checkDate")

  override def * = (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <>(BalanceCheck.tupled, BalanceCheck.unapply)
}

object BalanceChecks extends ForwardingEntityManager[BalanceCheck, BalanceChecks](
  EntityManager.create[BalanceCheck, BalanceChecks](
    tag => new BalanceChecks(tag), tableName = "BALANCE_CHECKS"))
