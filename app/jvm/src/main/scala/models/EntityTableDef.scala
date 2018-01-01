package models

import java.nio.ByteBuffer

import api.Picklers._
import boopickle.Default.{Pickle, Unpickle}
import common.accounting.Tags
import common.time.LocalDateTime
import models.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import models.SlickUtils.localDateTimeToSqlDateMapper
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.modification.{EntityModification, EntityModificationEntity}
import models.money.ExchangeRateMeasurement
import models.user.User

import scala.collection.immutable.Seq

sealed private[models] trait EntityTableDef[E <: Entity] {
  def tableName: String
  def table(tag: SlickTag): EntityTableDef.EntityTable[E]
}

private[models] object EntityTableDef {

  /** Table extension to be used with an Entity model. */
  // Based on active-slick (https://github.com/strongtyped/active-slick)
  sealed abstract class EntityTable[E <: Entity](
      tag: SlickTag,
      tableName: String,
      schemaName: Option[String] = None)(implicit val colType: BaseColumnType[Long])
      extends SlickTable[E](tag, schemaName, tableName) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  }

  implicit object UserDef extends EntityTableDef[User] {

    override val tableName: String = "USERS"
    override def table(tag: SlickTag): EntityTable[User] = new Table(tag)

    final class Table(tag: SlickTag) extends EntityTable[User](tag, tableName) {
      def loginName = column[String]("loginName")

      def passwordHash = column[String]("passwordHash")

      def name = column[String]("name")

      def databaseEncryptionKey = column[String]("databaseEncryptionKey")

      def expandCashFlowTablesByDefault = column[Boolean]("expandCashFlowTablesByDefault")

      override def * =
        (loginName, passwordHash, name, databaseEncryptionKey, expandCashFlowTablesByDefault, id.?) <>
          (User.tupled, User.unapply)
    }
  }

  implicit object TransactionDef extends EntityTableDef[Transaction] {

    override val tableName: String = "TRANSACTIONS"
    override def table(tag: SlickTag): EntityTable[Transaction] = new Table(tag)

    private implicit val tagsSeqToStringMapper: ColumnType[Seq[String]] = {
      MappedColumnType.base[Seq[String], String](Tags.serializeToString, Tags.parseTagsString)
    }

    final class Table(tag: SlickTag) extends EntityTable[Transaction](tag, tableName) {
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

  implicit object TransactionGroupDef extends EntityTableDef[TransactionGroup] {

    override val tableName = "TRANSACTION_GROUPS"
    override def table(tag: SlickTag): EntityTable[TransactionGroup] = new Table(tag)

    final class Table(tag: SlickTag) extends EntityTable[TransactionGroup](tag, tableName) {
      def createdDate = column[LocalDateTime]("createdDate")

      override def * = (createdDate, id.?) <> (TransactionGroup.tupled, TransactionGroup.unapply)
    }
  }

  implicit object BalanceCheckDef extends EntityTableDef[BalanceCheck] {

    override val tableName: String = "BALANCE_CHECKS"
    override def table(tag: SlickTag): EntityTable[BalanceCheck] = new Table(tag)

    final class Table(tag: SlickTag) extends EntityTable[BalanceCheck](tag, tableName) {
      def issuerId = column[Long]("issuerId")
      def moneyReservoirCode = column[String]("moneyReservoirCode")
      def balance = column[Long]("balance")
      def createdDate = column[LocalDateTime]("createdDate")
      def checkDate = column[LocalDateTime]("checkDate")

      override def * =
        (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <> (BalanceCheck.tupled, BalanceCheck.unapply)
    }
  }

  implicit object ExchangeRateMeasurementDef extends EntityTableDef[ExchangeRateMeasurement] {

    override val tableName: String = "EXCHANGE_RATE_MEASUREMENT"
    override def table(tag: SlickTag): EntityTable[ExchangeRateMeasurement] = new Table(tag)

    final class Table(tag: SlickTag) extends EntityTable[ExchangeRateMeasurement](tag, tableName) {
      def date = column[LocalDateTime]("date")
      def foreignCurrencyCode = column[String]("foreignCurrencyCode")
      def ratioReferenceToForeignCurrency = column[Double]("ratioReferenceToForeignCurrency")

      override def * =
        (date, foreignCurrencyCode, ratioReferenceToForeignCurrency, id.?) <> (ExchangeRateMeasurement.tupled, ExchangeRateMeasurement.unapply)
    }
  }

  implicit object EntityModificationEntityDef extends EntityTableDef[EntityModificationEntity] {

    override val tableName: String = "ENTITY_MODIFICATION_ENTITY"
    override def table(tag: SlickTag): EntityTable[EntityModificationEntity] = new Table(tag)

    final class Table(tag: SlickTag) extends EntityTable[EntityModificationEntity](tag, tableName) {
      def userId = column[Long]("userId")
      def change = column[EntityModification]("modification")
      def date = column[LocalDateTime]("date")

      override def * =
        (userId, change, date, id.?) <> (EntityModificationEntity.tupled, EntityModificationEntity.unapply)
    }

    implicit val entityModificationToBytesMapper: ColumnType[EntityModification] = {
      def toBytes(modification: EntityModification) = {
        val byteBuffer = Pickle.intoBytes(modification)

        val byteArray = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(byteArray)
        byteArray
      }
      def toEntityModification(bytes: Array[Byte]) = {
        val byteBuffer = ByteBuffer.wrap(bytes)
        Unpickle[EntityModification].fromBytes(byteBuffer)
      }
      MappedColumnType.base[EntityModification, Array[Byte]](toBytes, toEntityModification)
    }
  }
}
