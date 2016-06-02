package models.accounting

import scala.collection.immutable.Seq

import org.joda.time.DateTime
import models.SlickUtils.dbApi._

import common.Clock
import models.SlickUtils.dbRun
import models.SlickUtils.JodaToSqlDateMapper
import models.manager.{Identifiable, EntityTable, QueryableEntityManager}


case class TransactionGroup(createdDate: DateTime = Clock.now,
                            idOption: Option[Long] = None) extends Identifiable[TransactionGroup] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions: Seq[Transaction] = dbRun(Transactions.all.newQuery.filter {
    _.transactionGroupId === id
  }.result).toList

  def isZeroSum: Boolean = transactions.map(_.flow).sum == Money(0)
}

case class TransactionGroupPartial(transactions: Seq[TransactionPartial],
                                   zeroSum: Boolean = false)

class TransactionGroups(tag: Tag) extends EntityTable[TransactionGroup](tag, TransactionGroups.tableName) {
  def createdDate = column[DateTime]("createdDate")

  override def * = (createdDate, id.?) <>(TransactionGroup.tupled, TransactionGroup.unapply)
}

object TransactionGroups {
  private val tableName: String = "TRANSACTION_GROUPS"
  val all = QueryableEntityManager.backedByDatabase[TransactionGroup, TransactionGroups](tag => new TransactionGroups(tag), tableName)
}
