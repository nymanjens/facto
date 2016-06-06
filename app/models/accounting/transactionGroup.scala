package models.accounting

import scala.collection.immutable.Seq

import org.joda.time.DateTime

import common.Clock
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.SlickUtils.JodaToSqlDateMapper
import models.manager.{EntityTable, Entity, EntityManager, ForwardingEntityManager}


case class TransactionGroup(createdDate: DateTime = Clock.now,
                            idOption: Option[Long] = None) extends Entity[TransactionGroup] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions: Seq[Transaction] = dbRun(Transactions.newQuery.filter(_.transactionGroupId === id)).toList

  def isZeroSum: Boolean = transactions.map(_.flow).sum == Money(0)
}

case class TransactionGroupPartial(transactions: Seq[TransactionPartial],
                                   zeroSum: Boolean = false)

class TransactionGroups(tag: Tag) extends EntityTable[TransactionGroup](tag, TransactionGroups.tableName) {
  def createdDate = column[DateTime]("createdDate")

  override def * = (createdDate, id.?) <>(TransactionGroup.tupled, TransactionGroup.unapply)
}

object TransactionGroups extends ForwardingEntityManager[TransactionGroup, TransactionGroups](
  EntityManager.create[TransactionGroup, TransactionGroups](
    tag => new TransactionGroups(tag), tableName = "TRANSACTION_GROUPS"))
