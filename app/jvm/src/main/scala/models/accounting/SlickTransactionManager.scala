package models.accounting

import com.google.inject._
import common.accounting.Tags
import common.time.LocalDateTime
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.{dbRun, localDateTimeToSqlDateMapper}
import models.accounting.SlickTransactionManager.{Transactions, tableName}
import models.manager.{EntityTable, ImmutableEntityManager, SlickEntityManager}

import scala.collection.immutable.Seq

final class SlickTransactionManager @Inject()()
    extends ImmutableEntityManager[Transaction, Transactions](
      SlickEntityManager.create[Transaction, Transactions](
        tag => new Transactions(tag),
        tableName = tableName
      ))
    with Transaction.Manager {

  override def findByGroupId(groupId: Long): Seq[Transaction] =
    dbRun(newQuery.filter(_.transactionGroupId === groupId)).toList
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
