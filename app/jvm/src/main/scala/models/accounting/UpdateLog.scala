package models.accounting

import common.time.LocalDateTime
import models._
import models.manager.{Entity, EntityManager}

import scala.collection.immutable.Seq

/** UpdateLog entities are immutable. */
case class UpdateLog(userId: Long, change: String, date: LocalDateTime, idOption: Option[Long] = None)
    extends Entity {
  require(userId > 0)
  require(!change.isEmpty)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def user(implicit entityAccess: EntityAccess): User = entityAccess.userManager.findById(userId)
}

object UpdateLog {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[UpdateLog] {
    /* Returns most recent n entries sorted from old to new. */
    def fetchLastNEntries(n: Int): Seq[UpdateLog]

    def addLog(user: User, operation: UpdateOperation, newOrDeletedValue: TransactionGroup)(
        implicit entityAccess: EntityAccess): Unit

    def addLog(user: User, operation: UpdateOperation, newOrDeletedValue: BalanceCheck)(
        implicit entityAccess: EntityAccess): Unit

  }

  sealed trait UpdateOperation
  object AddNew extends UpdateOperation
  object Edit extends UpdateOperation
  object Delete extends UpdateOperation
}
