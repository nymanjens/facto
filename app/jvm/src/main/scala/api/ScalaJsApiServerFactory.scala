package api

import api.Picklers._
import api.ScalaJsApi.{
  GetAllEntitiesResponse,
  GetEntityModificationsResponse,
  GetInitialDataResponse,
  UpdateToken
}
import com.google.inject._
import common.PlayI18n
import common.money.Currency
import common.time.{Clock, LocalDateTime}
import models.SlickUtils.dbApi._
import models.SlickUtils.{dbRun, localDateTimeToSqlDateMapper}
import models.accounting.config.Config
import models.modification.{EntityModification, EntityType, SlickEntityModificationEntityManager}
import models.modificationhandler.EntityModificationHandler
import models.user.User
import models.{Entity, SlickEntityAccess}

import scala.collection.immutable.{Seq, TreeMap}
import scala.collection.mutable

final class ScalaJsApiServerFactory @Inject()(
    implicit accountingConfig: Config,
    clock: Clock,
    entityAccess: SlickEntityAccess,
    i18n: PlayI18n,
    entityModificationHandler: EntityModificationHandler,
    entityModificationManager: SlickEntityModificationEntityManager) {

  def create()(implicit user: User): ScalaJsApi = new ScalaJsApi() {

    override def getInitialData() =
      GetInitialDataResponse(
        accountingConfig = accountingConfig,
        user = user,
        allUsers = entityAccess.userManager.fetchAllSync(),
        i18nMessages = i18n.allI18nMessages,
        ratioReferenceToForeignCurrency = {
          val mapBuilder =
            mutable.Map[Currency, mutable.Builder[(LocalDateTime, Double), TreeMap[LocalDateTime, Double]]]()
          for (measurement <- entityAccess.exchangeRateMeasurementManager.fetchAllSync()) {
            val currency = measurement.foreignCurrency
            if (!(mapBuilder contains currency)) {
              mapBuilder(currency) = TreeMap.newBuilder[LocalDateTime, Double]
            }
            mapBuilder(currency) += (measurement.date -> measurement.ratioReferenceToForeignCurrency)
          }
          mapBuilder.toStream.map { case (k, v) => k -> v.result() }.toMap
        }
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = clock.now
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType => {
            entityType -> entityAccess.getManager(entityType).fetchAllSync()
          })
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def getEntityModifications(updateToken: UpdateToken): GetEntityModificationsResponse = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = clock.now

      val modifications = {
        val modificationEntities = dbRun(
          entityModificationManager.newQuery
            .filter(_.date >= updateToken)
            .sortBy(_.date))
        modificationEntities.toStream.map(_.modification).toVector
      }

      GetEntityModificationsResponse(modifications, nextUpdateToken)
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = {
      entityModificationHandler.persistEntityModifications(modifications)
    }
  }
}
