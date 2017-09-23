package models.accounting

import com.google.inject._
import com.google.common.base.Splitter
import com.google.common.hash.{HashCode, Hashing}
import common.time.Clock
import common.accounting.Tags
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.SlickUtils.dbRun
import models.SlickUtils.dbApi._
import models.SlickUtils.dbApi.{Tag => SlickTag}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import models.accounting.money.{DatedMoney, Money}
import models.manager.{Entity, SlickEntityManager, EntityTable, ImmutableEntityManager}
import models._
import common.time.LocalDateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.util.Try

import SlickTransactionManager.{Transactions, tableName}

final class SlickTransactionManager @Inject()(tagEntityManager: SlickTagEntityManager)
    extends ImmutableEntityManager[Transaction, Transactions](
      SlickEntityManager.create[Transaction, Transactions](
        tag => new Transactions(tag),
        tableName = tableName
      ))
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] =
    dbRun(newQuery.filter(_.transactionGroupId === groupId)).toList

  // ********** Mutators ********** //
  // Overriding mutators to update the tagEntityManager table
  override def add(transaction: Transaction): Transaction = {
    val persistedTransaction = super.add(transaction)

    // Add tagEntityManager to database
    for (tag <- persistedTransaction.tags) {
      tagEntityManager.add(TagEntity(name = tag, transactionId = persistedTransaction.id))
    }

    persistedTransaction
  }

  override def delete(transaction: Transaction): Unit = {
    // Remove tagEntityManager from database
    val tags = dbRun(tagEntityManager.newQuery.filter(_.transactionId === transaction.id))
    for (tag <- tags) {
      tagEntityManager.delete(tag)
    }

    super.delete(transaction)
  }
}

object SlickTransactionManager {
  private val tableName: String = "TRANSACTIONS"

  private implicit val tagsSeqToStringMapper: ColumnType[Seq[String]] = {
    MappedColumnType.base[Seq[String], String](Tags.serializeToString, Tags.parseTagsString)
  }

  final class Transactions(tag: SlickTag) extends EntityTable[Transaction](tag, tableName) {
    def transactionGroupId = column[Long]("transactionGroupId")
    def issuerId = column[Long]("issuerId")
    def beneficiaryAccountCode = column[String]("beneficiaryAccountCode")
    def moneyReservoirCode = column[String]("moneyReservoirCode")
    def categoryCode = column[String]("categoryCode")
    def description = column[String]("description")
    def flow = column[Long]("flow")
    def detailDescription = column[String]("detailDescription")
    def tags = column[Seq[String]]("tagsString")(tagsSeqToStringMapper)
    def createdDate = column[LocalDateTime]("createdDate")
    def transactionDate = column[LocalDateTime]("transactionDate")
    def consumedDate = column[LocalDateTime]("consumedDate")

    override def * =
      (
        transactionGroupId,
        issuerId,
        beneficiaryAccountCode,
        moneyReservoirCode,
        categoryCode,
        description,
        flow,
        detailDescription,
        tags,
        createdDate,
        transactionDate,
        consumedDate,
        id.?) <> (Transaction.tupled, Transaction.unapply)
  }
}
