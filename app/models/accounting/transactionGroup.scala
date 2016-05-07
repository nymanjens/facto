package models.accounting

import scala.collection.immutable.Seq

import org.joda.time.DateTime
import models.SlickUtils.dbApi._

import common.Clock
import models.SlickUtils.dbRun
import models.activeslick._
import models.SlickUtils.JodaToSqlDateMapper


case class TransactionGroup(createdDate: DateTime = Clock.now,
                            id: Option[Long] = None) extends Identifiable[TransactionGroup] {

  override def withId(id: Long) = copy(id = Some(id))

  def transactions: Seq[Transaction] = dbRun(Transactions.all.filter {
    _.transactionGroupId === id
  }.result).toList

  def isZeroSum: Boolean = transactions.map(_.flow).sum == Money(0)
}

case class TransactionGroupPartial(transactions: Seq[TransactionPartial],
                                   zeroSum: Boolean = false)

class TransactionGroups(tag: Tag) extends EntityTable[TransactionGroup](tag, "TRANSACTION_GROUPS") {
  def createdDate = column[DateTime]("createdDate")

  override def * = (createdDate, id.?) <>(TransactionGroup.tupled, TransactionGroup.unapply)
}

object TransactionGroups {
  val all = new EntityTableQuery[TransactionGroup, TransactionGroups](tag => new TransactionGroups(tag))
}
