package models.accounting

import org.joda.time.DateTime
import slick.driver.H2Driver.api._

import common.Clock
import models.ModelUtils.dbRun
import models.activeslick._
import models.ModelUtils.JodaToSqlDateMapper


case class TransactionGroup(createdDate: DateTime = Clock.now,
                            id: Option[Long] = None) extends Identifiable[TransactionGroup] {

  override def withId(id: Long) = copy(id = Some(id))

  def transactions: Seq[Transaction] = dbRun(Transactions.all.filter {
    _.transactionGroupId === id
  }.result)

  def isZeroSum: Boolean = transactions.map(_.flow).sum == Money(0)
}

class TransactionGroups(tag: Tag) extends EntityTable[TransactionGroup](tag, "TRANSACTION_GROUPS") {
  def createdDate = column[DateTime]("createdDate")

  def * = (createdDate, id.?) <>(TransactionGroup.tupled, TransactionGroup.unapply)
}

object TransactionGroups {
  val all = new EntityTableQuery[TransactionGroup, TransactionGroups](tag => new TransactionGroups(tag))
}
