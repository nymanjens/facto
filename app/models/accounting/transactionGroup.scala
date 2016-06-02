package models.accounting

import scala.collection.immutable.Seq

import org.joda.time.DateTime

import common.Clock
import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.SlickUtils.JodaToSqlDateMapper
import models.manager.{EntityTable, Identifiable, EntityManager, QueryableEntityManager, ForwardingEntityManager}


case class TransactionGroup(createdDate: DateTime = Clock.now,
                            idOption: Option[Long] = None) extends Identifiable[TransactionGroup] {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions: Seq[Transaction] = Transactions.fetchAll(_.filter(_.transactionGroupId == id))

  def isZeroSum: Boolean = transactions.map(_.flow).sum == Money(0)
}

case class TransactionGroupPartial(transactions: Seq[TransactionPartial],
                                   zeroSum: Boolean = false)

class TransactionGroups(tag: Tag) extends EntityTable[TransactionGroup](tag, TransactionGroups.tableName) {
  def createdDate = column[DateTime]("createdDate")

  override def * = (createdDate, id.?) <>(TransactionGroup.tupled, TransactionGroup.unapply)
}

//object TransactionGroups  extends ForwardingEntityManager[TransactionGroup](
//  EntityManager.caching(
//    QueryableEntityManager.backedByDatabase[TransactionGroup, TransactionGroups](
//      tag => new TransactionGroups(tag), tableName = "TRANSACTION_GROUPS")))

import models.manager.ForwardingQueryableEntityManager

object TransactionGroups extends ForwardingQueryableEntityManager[TransactionGroup, TransactionGroups](
  QueryableEntityManager.backedByDatabase[TransactionGroup, TransactionGroups](
    tag => new TransactionGroups(tag), tableName = "TRANSACTION_GROUPS"))
