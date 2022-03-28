package hydro.controllers

import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.instantToSqlTimestampMapper
import hydro.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import hydro.models.slick.SlickUtils.dbRun
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.common.time.Clock
import hydro.controllers.helpers.AuthenticatedAction
import hydro.models.modification.EntityModificationEntity
import hydro.models.slick.SlickUtils
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import java.time.Duration

final class StandardActions @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    entityAccess: JvmEntityAccess,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
    clock: Clock,
) extends AbstractController(components)
    with I18nSupport {

  def index() = AuthenticatedAction { implicit user => implicit request =>
    Redirect(hydro.controllers.routes.StandardActions.reactAppRoot())
  }

  def reactAppRoot = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.reactApp())
  }

  def reactApp(anyString: String) = reactAppRoot

  def reactAppWithoutCredentials = Action { implicit request =>
    Ok(views.html.reactApp())
  }

  def healthCheck = Action { implicit request =>
    entityAccess.checkConsistentCaches()
    Ok("OK")
  }

  def databaseSchema(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    val schema = entityAccess.getDatabaseSchemaAsStrings().mkString("\n\n")

    Ok(s"OK\n\n$schema")
  }

  def clearOldEntityModifications(dryOrWetRun: String, applicationSecret: String) = Action {
    implicit request =>
      validateApplicationSecret(applicationSecret)

      val cutoffTime = clock.nowInstant.minus(Duration.ofDays(182))

      val totalCount = dbRun(entityAccess.newSlickQuery[EntityModificationEntity]().map(_.instantNanos)).length
      val oldModificationsCount = dbRun(
        entityAccess
          .newSlickQuery[EntityModificationEntity]()
          .filter(_.instant < cutoffTime)
          .map(_.instantNanos)
      ).length

      val extraInfo = if (dryOrWetRun == "wet") {
        val result: Int = dbRun(
          entityAccess
            .newSlickQuery[EntityModificationEntity]()
            .filter(_.instant < cutoffTime)
            .delete
        )

        s"Successfully removed $result old modifications\n"
      } else {
        ""
      }

      Ok(
        "OK\n\n" + s"Total number of entity modifications: $totalCount\n" +
          s"Number of entity modifications to be removed: $oldModificationsCount\n\n" + extraInfo
      )
  }

  // ********** private helper methods ********** //
  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'",
    )
  }
}
