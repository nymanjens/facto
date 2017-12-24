package models.access

import models.modification.EntityType._
import java.util.Objects

import common.time.LocalDateTime
import models.Entity
import models.access.ModelField.FieldType
import models.modification.EntityType
import models.money.ExchangeRateMeasurement
import models.user.User

import scala.collection.immutable.Seq

/**
  * Represents a field in an model entity.
  *
  * @param name A name for this field that is unique in E
  * @tparam V The type of the values
  * @tparam E The type corresponding to the entity that contains this field
  */
sealed abstract class ModelField[V, E] private[access] (val name: String, accessor: E => V)(
    implicit val fieldType: FieldType[V]) {

  def get(entity: E): V = accessor(entity)

  override def toString = name

  override def equals(any: scala.Any) = {
    any match {
      case that: ModelField[_, _] => this.name == that.name
      case _ => false
    }

  }
  override def hashCode() = Objects.hash(name)
}

object ModelField {

  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case UserType => User.id.asInstanceOf[ModelField[Long, E]]
    case TransactionType => Transaction.id.asInstanceOf[ModelField[Long, E]]
    case TransactionGroupType => TransactionGroup.id.asInstanceOf[ModelField[Long, E]]
    case BalanceCheckType => BalanceCheck.id.asInstanceOf[ModelField[Long, E]]
    case ExchangeRateMeasurementType => ExchangeRateMeasurement.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Related types **************** //
  sealed trait FieldType[T]
  object FieldType {
    implicit case object BooleanType extends FieldType[Boolean]
    implicit case object LongType extends FieldType[Long]
    implicit case object DoubleType extends FieldType[Double]
    implicit case object StringType extends FieldType[String]
    implicit case object LocalDateTimeType extends FieldType[LocalDateTime]
    implicit case object StringSeqType extends FieldType[Seq[String]]
  }

  abstract sealed class IdModelField[E <: Entity] extends ModelField[Long, E]("id", _.idOption getOrElse -1)

  // **************** Enumeration of all fields **************** //
  object User {
    private type E = User

    case object id extends IdModelField[E]
    case object loginName extends ModelField[String, E]("loginName", _.loginName)
    case object passwordHash extends ModelField[String, E]("passwordHash", _.passwordHash)
    case object name extends ModelField[String, E]("name", _.name)
    case object databaseEncryptionKey
        extends ModelField[String, E]("databaseEncryptionKey", _.databaseEncryptionKey)
    case object expandCashFlowTablesByDefault
        extends ModelField[Boolean, E]("expandCashFlowTablesByDefault", _.expandCashFlowTablesByDefault)
  }

  object Transaction {
    private type E = models.accounting.Transaction

    case object id extends IdModelField[E]
    case object transactionGroupId extends ModelField[Long, E]("transactionGroupId", _.transactionGroupId)
    case object issuerId extends ModelField[Long, E]("issuerId", _.issuerId)
    case object beneficiaryAccountCode
        extends ModelField[String, E]("beneficiaryAccountCode", _.beneficiaryAccountCode)
    case object moneyReservoirCode extends ModelField[String, E]("moneyReservoirCode", _.moneyReservoirCode)
    case object categoryCode extends ModelField[String, E]("categoryCode", _.categoryCode)
    case object description extends ModelField[String, E]("description", _.description)
    case object flowInCents extends ModelField[Long, E]("flowInCents", _.flowInCents)
    case object detailDescription extends ModelField[String, E]("detailDescription", _.detailDescription)
    case object tags extends ModelField[Seq[String], E]("tags", _.tags)
    case object createdDate extends ModelField[LocalDateTime, E]("createdDate", _.createdDate)
    case object transactionDate extends ModelField[LocalDateTime, E]("transactionDate", _.transactionDate)
    case object consumedDate extends ModelField[LocalDateTime, E]("consumedDate", _.consumedDate)
  }

  object TransactionGroup {
    private type E = models.accounting.TransactionGroup

    case object id extends IdModelField[E]
    case object createdDate extends ModelField[LocalDateTime, E]("createdDate", _.createdDate)
  }

  object BalanceCheck {
    private type E = models.accounting.BalanceCheck

    case object id extends IdModelField[E]
    case object issuerId extends ModelField[Long, E]("issuerId", _.issuerId)
    case object moneyReservoirCode extends ModelField[String, E]("moneyReservoirCode", _.moneyReservoirCode)
    case object balanceInCents extends ModelField[Long, E]("balanceInCents", _.balanceInCents)
    case object createdDate extends ModelField[LocalDateTime, E]("createdDate", _.createdDate)
    case object checkDate extends ModelField[LocalDateTime, E]("checkDate", _.checkDate)
  }

  object ExchangeRateMeasurement {
    private type E = ExchangeRateMeasurement

    case object id extends IdModelField[E]
    case object date extends ModelField[LocalDateTime, E]("date", _.date)
    case object foreignCurrencyCode
        extends ModelField[String, E]("foreignCurrencyCode", _.foreignCurrencyCode)
    case object ratioReferenceToForeignCurrency
        extends ModelField[Double, E]("ratioReferenceToForeignCurrency", _.ratioReferenceToForeignCurrency)
  }
}
