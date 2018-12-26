package app.api

import app.api.Picklers._
import app.api.ScalaJsApi._
import common.money.Currency
import common.time.LocalDateTime
import models.Entity
import models.accounting.config.Config
import models.modification.EntityModification
import models.modification.EntityType
import models.user.User

import scala.collection.SortedMap
import scala.collection.immutable.Seq

/** API for communication between client and server (clients calls server). */
trait ScalaJsApi {

  /** Returns most information needed to render the first page. */
  def getInitialData(): GetInitialDataResponse

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType.any]): GetAllEntitiesResponse

  /** Stores the given entity modifications. */
  def persistEntityModifications(modifications: Seq[EntityModification]): Unit

  def executeDataQuery(dbQuery: PicklableDbQuery): Seq[Entity]

  def executeCountQuery(dbQuery: PicklableDbQuery): Int

  /** Adds or updates a user according to the present fields in the given prototype. */
  def upsertUser(userPrototype: UserPrototype): Unit
}

object ScalaJsApi {
  type UpdateToken = String

  /**
    * @param i18nMessages Maps key to the message with placeholders.
    * @param nextUpdateToken An update token for all changes since this call
    */
  case class GetInitialDataResponse(
      accountingConfig: Config,
      user: User,
      allUsers: Seq[User],
      i18nMessages: Map[String, String],
      ratioReferenceToForeignCurrency: Map[Currency, SortedMap[LocalDateTime, Double]],
      nextUpdateToken: UpdateToken)

  case class GetAllEntitiesResponse(entitiesMap: Map[EntityType.any, Seq[Entity]],
                                    nextUpdateToken: UpdateToken) {
    def entityTypes: Iterable[EntityType.any] = entitiesMap.keys
    def entities[E <: Entity](entityType: EntityType[E]): Seq[E] = {
      entitiesMap(entityType).asInstanceOf[Seq[E]]
    }
  }
  case class ModificationsWithToken(modifications: Seq[EntityModification], nextUpdateToken: UpdateToken)

  /**
    * Copy of the User model with all fields optional.
    *
    * @param id Required for update. Unset for add.
    * @param loginName Required for add.
    * @param plainTextPassword Required for add.
    * @param name Required for add.
    */
  case class UserPrototype(id: Option[Long] = None,
                           loginName: Option[String] = None,
                           plainTextPassword: Option[String] = None,
                           name: Option[String] = None,
                           isAdmin: Option[Boolean] = None,
                           expandCashFlowTablesByDefault: Option[Boolean] = None,
                           expandLiquidationTablesByDefault: Option[Boolean] = None)
  object UserPrototype {
    def create(id: java.lang.Long = null,
               loginName: String = null,
               plainTextPassword: String = null,
               name: String = null,
               isAdmin: java.lang.Boolean = null,
               expandCashFlowTablesByDefault: java.lang.Boolean = null,
               expandLiquidationTablesByDefault: java.lang.Boolean = null): UserPrototype =
      UserPrototype(
        id = if (id == null) None else Some(id),
        loginName = Option(loginName),
        plainTextPassword = Option(plainTextPassword),
        name = Option(name),
        isAdmin = if (isAdmin == null) None else Some(isAdmin),
        expandCashFlowTablesByDefault =
          if (expandCashFlowTablesByDefault == null) None else Some(expandCashFlowTablesByDefault),
        expandLiquidationTablesByDefault =
          if (expandLiquidationTablesByDefault == null) None else Some(expandLiquidationTablesByDefault)
      )
  }
}
