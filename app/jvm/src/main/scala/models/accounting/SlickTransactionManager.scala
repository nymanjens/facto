package models.accounting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.async.Async.{async, await}
import com.google.inject._
import common.accounting.Tags
import common.time.LocalDateTime
import models.EntityTable
import models.SlickUtils.dbApi.{Tag => SlickTag, _}
import models.SlickUtils.{dbRun, localDateTimeToSqlDateMapper, database}
import models.accounting.SlickTransactionManager.{Transactions, tableName}
import models.manager.{ImmutableEntityManager, SlickEntityManager}

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class SlickTransactionManager @Inject()()
    extends ImmutableEntityManager[Transaction, Transactions](
      SlickEntityManager.create[Transaction, Transactions](
        tag => new Transactions(tag),
        tableName = tableName
      ))
    with Transaction.Manager

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
