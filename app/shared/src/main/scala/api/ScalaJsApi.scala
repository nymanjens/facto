package api

import api.Picklers._
import api.ScalaJsApi.{GetAllEntitiesResponse, ModificationsWithToken, GetInitialDataResponse, UpdateToken}
import common.money.Currency
import common.time.LocalDateTime
import models.Entity
import models.access.DbQuery
import models.accounting.config.Config
import models.modification.{EntityModification, EntityType}
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
}
