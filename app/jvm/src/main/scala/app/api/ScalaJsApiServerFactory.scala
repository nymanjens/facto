package app.api

import app.api.ScalaJsApi._
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.PlayI18n
import app.common.money.Currency
import app.models.access.JvmEntityAccess
import app.models.accounting.config.Config
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import app.models.user.Users
import com.google.inject._
import hydro.api.EntityPermissions
import hydro.api.PicklableDbQuery
import hydro.common.time.Clock
import hydro.common.time.LocalDateTime
import hydro.models.Entity
import hydro.models.access.DbQuery

import scala.collection.immutable.Seq
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

final class ScalaJsApiServerFactory @Inject() (implicit
    accountingConfig: Config,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    i18n: PlayI18n,
    entityPermissions: EntityPermissions,
) {

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
        nextUpdateToken = toUpdateToken(clock.nowInstant),
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = toUpdateToken(clock.nowInstant)
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType =>
            entityType -> {
              val allEntities = entityAccess.newQuerySync()(entityType).data()

              // apply permissions filter
              allEntities.filter(entityPermissions.isAllowedToRead)
            }
          )
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def persistEntityModifications(
        modifications: Seq[EntityModification],
        waitUntilQueryReflectsModifications: Boolean,
    ): Unit = {
      // check permissions
      for (modification <- modifications) {
        entityPermissions.checkAllowedForWrite(modification)
      }

      val future = entityAccess.persistEntityModificationsAsync(modifications)
      if (waitUntilQueryReflectsModifications) {
        Await.ready(future, atMost = Duration.Inf)
      }
    }

    override def executeDataQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        val allData = entityAccess.queryExecutor[E].data(query)

        // apply permissions filter
        allData.filter(entityPermissions.isAllowedToRead)
      }
      internal
    }

    override def executeCountQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].count(query)

        // Don't apply permissions filter, since only metadata is leaked
      }
      internal
    }

    override def upsertUser(userProto: UserPrototype): Unit = {
      def requireNonEmpty(s: Option[String]): Unit = {
        require(s.isDefined, "field is missing")
        require(s.get.nonEmpty, "field contains an empty string")
      }
      def requireNonEmptyIfSet(s: Option[String]): Unit = {
        if (s.isDefined) {
          require(s.get.nonEmpty, "field contains an empty string")
        }
      }

      userProto.id match {
        case None => // Add user
          requireNonEmpty(userProto.loginName)
          requireNonEmpty(userProto.plainTextPassword)
          requireNonEmpty(userProto.name)

          // Check permissions
          require(user.isAdmin, "Only an admin can add users")

          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(
              Users.createUser(
                loginName = userProto.loginName.get,
                password = userProto.plainTextPassword.get,
                name = userProto.name.get,
                isAdmin = userProto.isAdmin getOrElse false,
                expandCashFlowTablesByDefault = userProto.expandCashFlowTablesByDefault getOrElse true,
                expandLiquidationTablesByDefault = userProto.expandLiquidationTablesByDefault getOrElse true,
              )
            )
          )

        case Some(id) => // Update user
          requireNonEmptyIfSet(userProto.loginName)
          requireNonEmptyIfSet(userProto.plainTextPassword)
          requireNonEmptyIfSet(userProto.name)

          // Check permissions
          require(user.isAdmin || id == user.id, "Changing an other user's password")

          val existingUser = entityAccess.newQuerySync[User]().findById(id)
          val updatedUser = {
            var result = existingUser.copy(
              loginName = userProto.loginName getOrElse existingUser.loginName,
              name = userProto.name getOrElse existingUser.name,
              isAdmin = userProto.isAdmin getOrElse existingUser.isAdmin,
              expandCashFlowTablesByDefault =
                userProto.expandCashFlowTablesByDefault getOrElse existingUser.expandCashFlowTablesByDefault,
              expandLiquidationTablesByDefault = userProto.expandLiquidationTablesByDefault getOrElse
                existingUser.expandLiquidationTablesByDefault,
            )
            if (userProto.plainTextPassword.isDefined) {
              result = Users.copyUserWithPassword(result, userProto.plainTextPassword.get)
            }
            result
          }

          entityAccess.persistEntityModifications(EntityModification.createUpdateAllFields(updatedUser))
      }
    }
  }
}
