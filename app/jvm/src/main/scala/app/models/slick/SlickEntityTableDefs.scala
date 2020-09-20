package app.models.slick

import app.models.accounting.BalanceCheck
import app.models.accounting.Transaction
import app.models.accounting.TransactionGroup
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.common.time.LocalDateTime
import hydro.common.Tags
import hydro.models.slick.SlickEntityTableDef
import hydro.models.slick.SlickEntityTableDef.EntityTable
import hydro.models.slick.SlickUtils._
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.dbApi.{Tag => SlickTag}
import hydro.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import hydro.models.slick.SlickUtils.finiteDurationToMillisMapper
import hydro.models.slick.SlickUtils.orderTokenToBytesMapper
import hydro.models.slick.SlickUtils.lastUpdateTimeToBytesMapper
import hydro.models.UpdatableEntity.LastUpdateTime

import scala.collection.immutable.Seq

object SlickEntityTableDefs {

  val all: Seq[SlickEntityTableDef[_]] =
    Seq(
      UserDef,
      TransactionDef,
      TransactionGroupDef,
      BalanceCheckDef,
      ExchangeRateMeasurementDef,
      EntityModificationEntityDef,
    )

  implicit object UserDef extends SlickEntityTableDef[User] {

    override val tableName: String = "USERS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[User](tag, tableName) {
      def loginName = column[String]("loginName")
      def passwordHash = column[String]("passwordHash")
      def name = column[String]("name")
      def isAdmin = column[Boolean]("isAdmin")
      def expandCashFlowTablesByDefault = column[Boolean]("expandCashFlowTablesByDefault")
      def expandLiquidationTablesByDefault = column[Boolean]("expandLiquidationTablesByDefault")
      def lastUpdateTime = column[LastUpdateTime]("lastUpdateTime")

      override def * =
        (
          loginName,
          passwordHash,
          name,
          isAdmin,
          expandCashFlowTablesByDefault,
          expandLiquidationTablesByDefault,
          id.?,
          lastUpdateTime,
        ) <> (User.tupled, User.unapply)
    }
  }

  implicit object TransactionDef extends SlickEntityTableDef[Transaction] {

    override val tableName: String = "TRANSACTIONS"
    override def table(tag: SlickTag): Table = new Table(tag)

    private implicit val tagsSeqToStringMapper: ColumnType[Seq[String]] = {
      MappedColumnType.base[Seq[String], String](Tags.serializeToString, Tags.parseTagsString)
    }

    /* override */
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
          id.?,
        ) <> (Transaction.tupled, Transaction.unapply)
    }
  }

  implicit object TransactionGroupDef extends SlickEntityTableDef[TransactionGroup] {

    override val tableName = "TRANSACTION_GROUPS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[TransactionGroup](tag, tableName) {
      def createdDate = column[LocalDateTime]("createdDate")

      override def * = (createdDate, id.?) <> (TransactionGroup.tupled, TransactionGroup.unapply)
    }
  }

  implicit object BalanceCheckDef extends SlickEntityTableDef[BalanceCheck] {

    override val tableName: String = "BALANCE_CHECKS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[BalanceCheck](tag, tableName) {
      def issuerId = column[Long]("issuerId")
      def moneyReservoirCode = column[String]("moneyReservoirCode")
      def balance = column[Long]("balance")
      def createdDate = column[LocalDateTime]("createdDate")
      def checkDate = column[LocalDateTime]("checkDate")

      override def * =
        (
          issuerId,
          moneyReservoirCode,
          balance,
          createdDate,
          checkDate,
          id.?,
        ) <> (BalanceCheck.tupled, BalanceCheck.unapply)
    }
  }

  implicit object ExchangeRateMeasurementDef extends SlickEntityTableDef[ExchangeRateMeasurement] {

    override val tableName: String = "EXCHANGE_RATE_MEASUREMENT"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[ExchangeRateMeasurement](tag, tableName) {
      def date = column[LocalDateTime]("date")
      def foreignCurrencyCode = column[String]("foreignCurrencyCode")
      def ratioReferenceToForeignCurrency = column[Double]("ratioReferenceToForeignCurrency")

      override def * =
        (
          date,
          foreignCurrencyCode,
          ratioReferenceToForeignCurrency,
          id.?,
        ) <> (ExchangeRateMeasurement.tupled, ExchangeRateMeasurement.unapply)
    }
  }
}
