package api

import java.time.{LocalDate, LocalTime}

import boopickle.Default._
import common.time.LocalDateTime
import models.accounting.config._
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.modification.EntityType._
import models.modification.EntityType
import models.manager.Entity
import models.modification.EntityModification
import models.money.ExchangeRateMeasurement
import models.user.User

import scala.collection.immutable.{Seq, Set}

object Picklers {

  // Pickler that does the same as an autogenerated User pickler, except that it redacts the user's password
  implicit object UserPickler extends Pickler[User] {
    override def pickle(user: User)(implicit state: PickleState): Unit = logExceptions {
      state.pickle(user.loginName)
      state.pickle(user.name)
      state.pickle(user.databaseEncryptionKey)
      state.pickle(user.idOption)
    }
    override def unpickle(implicit state: UnpickleState): User = logExceptions {
      User(
        loginName = state.unpickle[String],
        passwordHash = "<redacted>",
        name = state.unpickle[String],
        databaseEncryptionKey = state.unpickle[String],
        idOption = state.unpickle[Option[Long]]
      )
    }
  }

  // This pickler should normally be autogenerated by boopickle, but there seems to be a bug
  // causing this not to work. Fixed by manually picking templates.
  implicit object TemplatePickler extends Pickler[Template] {
    override def pickle(template: Template)(implicit state: PickleState): Unit = logExceptions {
      state.pickle(template.code)
      state.pickle(template.name)
      state.pickle(template.placement.map(_.toString))
      state.pickle(template.onlyShowForUserLoginNames)
      state.pickle(template.zeroSum)
      state.pickle(template.iconClass)
      state.pickle(template.transactions)
    }
    override def unpickle(implicit state: UnpickleState): Template = logExceptions {
      Template(
        code = state.unpickle[String],
        name = state.unpickle[String],
        placement = state.unpickle[Set[String]].map(Template.Placement.fromString),
        onlyShowForUserLoginNames = state.unpickle[Option[Set[String]]],
        zeroSum = state.unpickle[Boolean],
        iconClass = state.unpickle[String],
        transactions = state.unpickle[Seq[Template.Transaction]]
      )
    }
  }

  implicit object LocalDateTimePickler extends Pickler[LocalDateTime] {
    override def pickle(dateTime: LocalDateTime)(implicit state: PickleState): Unit = logExceptions {
      val date = dateTime.toLocalDate
      val time = dateTime.toLocalTime

      state.pickle(date.getYear)
      state.pickle(date.getMonth.getValue)
      state.pickle(date.getDayOfMonth)
      state.pickle(time.getHour)
      state.pickle(time.getMinute)
      state.pickle(time.getSecond)
    }
    override def unpickle(implicit state: UnpickleState): LocalDateTime = logExceptions {
      LocalDateTime.of(
        LocalDate.of(
          state.unpickle[Int] /* year */,
          state.unpickle[Int] /* month */,
          state.unpickle[Int] /* dayOfMonth */
        ),
        LocalTime.of(
          state.unpickle[Int] /* hour */,
          state.unpickle[Int] /* minute */,
          state.unpickle[Int] /* second */
        )
      )
    }
  }

  implicit object EntityTypePickler extends Pickler[EntityType.any] {
    override def pickle(entityType: EntityType.any)(implicit state: PickleState): Unit = logExceptions {
      val intValue: Int = entityType match {
        case UserType => 1
        case TransactionType => 2
        case TransactionGroupType => 3
        case BalanceCheckType => 4
        case ExchangeRateMeasurementType => 5
      }
      state.pickle(intValue)
    }
    override def unpickle(implicit state: UnpickleState): EntityType.any = logExceptions {
      state.unpickle[Int] match {
        case 1 => UserType
        case 2 => TransactionType
        case 3 => TransactionGroupType
        case 4 => BalanceCheckType
        case 5 => ExchangeRateMeasurementType
      }
    }
  }

  implicit val entityPickler = compositePickler[Entity]
    .addConcreteType[User]
    .addConcreteType[Transaction]
    .addConcreteType[TransactionGroup]
    .addConcreteType[BalanceCheck]
    .addConcreteType[ExchangeRateMeasurement]

  implicit object EntityModificationPickler extends Pickler[EntityModification] {
    val addNumber = 1
    val removeNumber = 2

    override def pickle(modification: EntityModification)(implicit state: PickleState): Unit =
      logExceptions {
        state.pickle[EntityType.any](modification.entityType)
        // Pickle number
        state.pickle(modification match {
          case _: EntityModification.Add[_] => addNumber
          case _: EntityModification.Remove[_] => removeNumber
        })
        modification match {
          case EntityModification.Add(entity) =>
            state.pickle(entity)
          case EntityModification.Remove(entityId) =>
            state.pickle(entityId)
        }
      }
    override def unpickle(implicit state: UnpickleState): EntityModification = logExceptions {
      val entityType = state.unpickle[EntityType.any]
      state.unpickle[Int] match {
        case `addNumber` =>
          val entity = state.unpickle[Entity]
          def addModification[E <: Entity](entity: Entity, entityType: EntityType[E]): EntityModification = {
            EntityModification.Add(entityType.checkRightType(entity))(entityType)
          }
          addModification(entity, entityType)
        case `removeNumber` =>
          val entityId = state.unpickle[Long]
          EntityModification.Remove(entityId)(entityType)
      }
    }
  }

  private def logExceptions[T](codeBlock: => T): T = {
    try {
      codeBlock
    } catch {
      case t: Throwable =>
        println(s"  Caught exception while pickling: $t")
        t.printStackTrace()
        throw t
    }
  }
}
