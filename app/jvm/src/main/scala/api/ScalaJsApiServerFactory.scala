package api

import api.Picklers._
import api.ScalaJsApi.{
  GetAllEntitiesResponse,
  ModificationsWithToken,
  GetInitialDataResponse,
  UpdateToken
}
import com.google.inject._
import common.PlayI18n
import common.money.Currency
import common.time.{Clock, LocalDateTime}
import models.Entity
import models.access.{DbQuery, JvmEntityAccess}
import models.accounting.config.Config
import models.modification.{EntityModification, EntityModificationEntity, EntityType}
import models.money.ExchangeRateMeasurement
import models.slick.SlickUtils.dbApi._
import models.slick.SlickUtils.{dbRun, localDateTimeToSqlDateMapper}
import models.user.User

import scala.collection.immutable.{Seq, TreeMap}
import scala.collection.mutable

final class ScalaJsApiServerFactory @Inject()(implicit accountingConfig: Config,
                                              clock: Clock,
                                              entityAccess: JvmEntityAccess,
                                              i18n: PlayI18n) {

  def create()(implicit user: User): ScalaJsApi = new ScalaJsApi() {

    override def getInitialData() =
      GetInitialDataResponse(
        accountingConfig = accountingConfig,
        user = user,
        allUsers = entityAccess.newQuerySync[User]().data(),
        i18nMessages = i18n.allI18nMessages,
        ratioReferenceToForeignCurrency = {
          val mapBuilder =
            mutable.Map[Currency, mutable.Builder[(LocalDateTime, Double), TreeMap[LocalDateTime, Double]]]()
          for (measurement <- entityAccess.newQuerySync[ExchangeRateMeasurement]().data()) {
            val currency = measurement.foreignCurrency
            if (!(mapBuilder contains currency)) {
              mapBuilder(currency) = TreeMap.newBuilder[LocalDateTime, Double]
            }
            mapBuilder(currency) += (measurement.date -> measurement.ratioReferenceToForeignCurrency)
          }
          mapBuilder.toStream.map { case (k, v) => k -> v.result() }.toMap
        },
        nextUpdateToken = clock.now
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = clock.now
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType => {
            entityType -> entityAccess.newQuerySync()(entityType).data()
          })
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def getEntityModifications(updateToken: UpdateToken): ModificationsWithToken = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = clock.now

      val modifications = {
        val modificationEntities = dbRun(
          entityAccess
            .newSlickQuery[EntityModificationEntity]()
            .filter(_.date >= updateToken)
            .sortBy(_.date))
        modificationEntities.toStream.map(_.modification).toVector
      }

      ModificationsWithToken(modifications, nextUpdateToken)
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = {
      entityAccess.persistEntityModifications(modifications)
    }

    override def executeDataQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].data(query)
      }
      internal
    }

    override def executeCountQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].count(query)
      }
      internal
    }
  }
}
