package api

import api.Picklers._
import api.ScalaJsApi.{GetAllEntitiesResponse, GetEntityModificationsResponse, GetInitialDataResponse, UpdateToken}
import common.time.LocalDateTime
import models.User
import models.accounting.config.Config
import models.manager.{Entity, EntityModification, EntityType}

import scala.collection.immutable.Seq

trait ScalaJsApi {

  def getInitialData(): GetInitialDataResponse

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType.any]): GetAllEntitiesResponse

  /** Returns all modifications that happened after the given update token was returned, ordered from old to new. */
  def getEntityModifications(updateToken: UpdateToken): GetEntityModificationsResponse

  def persistEntityModifications(modifications: Seq[EntityModification]): Unit
}

object ScalaJsApi {
  type UpdateToken = LocalDateTime

  /**
    * @param i18nMessages Maps key to the message with placeholders.
    */
  case class GetInitialDataResponse(accountingConfig: Config, user: User, i18nMessages: Map[String, String])

  case class GetAllEntitiesResponse(entitiesMap: Map[EntityType.any, Seq[Entity]],
                                    nextUpdateToken: UpdateToken) {
    def entityTypes: Iterable[EntityType.any] = entitiesMap.keys
    def entities[E <: Entity](entityType: EntityType[E]): Seq[E] = {
      entitiesMap(entityType).asInstanceOf[Seq[E]]
    }
  }
  case class GetEntityModificationsResponse(modifications: Seq[EntityModification],
                                            nextUpdateToken: UpdateToken)
}
