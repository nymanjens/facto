package controllers

import java.nio.ByteBuffer

import api.Picklers._
import api.ScalaJsApi.UpdateToken
import api.{PicklableDbQuery, ScalaJsApiRequest, ScalaJsApiServerFactory}
import boopickle.Default._
import com.google.inject.Inject
import controllers.Application.Forms
import controllers.Application.Forms.{AddUserData, ChangePasswordData}
import controllers.helpers.AuthenticatedAction
import models.modification.EntityType
import models.Entity
import akka.stream.scaladsl._
import common.ResourceFiles
import models.access.{DbQuery, JvmEntityAccess}
import models.modification.EntityModification
import models.user.{User, Users}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.collection.immutable.Seq

final class Application @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  entityAccess: JvmEntityAccess,
                                  scalaJsApiServerFactory: ScalaJsApiServerFactory,
                                  playConfiguration: play.api.Configuration,
                                  env: play.api.Environment,
                                  webJarAssets: controllers.WebJarAssets)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions: HTTP pages ********** //
  def index() = AuthenticatedAction { implicit user => implicit request =>
    Redirect(controllers.routes.Application.reactAppRoot())
  }

  def profile() = AuthenticatedAction { implicit user => implicit request =>
    val initialData = ChangePasswordData(user.loginName)
    Ok(views.html.profile(Forms.changePasswordForm.fill(initialData)))
  }

  def changePassword = AuthenticatedAction { implicit user => implicit request =>
    Forms.changePasswordForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.profile(formWithErrors)), {
        case ChangePasswordData(loginName, _, password, _) =>
          require(loginName == user.loginName)
          entityAccess.persistEntityModifications(
            EntityModification.createUpdate(Users.copyUserWithPassword(user, password)))
          val message = Messages("facto.successfully-updated-password")
          Redirect(routes.Application.profile()).flashing("message" -> message)
      }
    )
  }

  def administration() = AuthenticatedAction.requireAdminUser { implicit user => implicit request =>
    Ok(views.html.administration(users = entityAccess.newQuerySync[User]().data(), Forms.addUserForm))
  }

  def addUser() = AuthenticatedAction.requireAdminUser { implicit user => implicit request =>
    Forms.addUserForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(
          views.html.administration(users = entityAccess.newQuerySync[User]().data(), formWithErrors)), {
        case AddUserData(loginName, name, password, _) =>
          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(Users.createUser(loginName, password, name)))
          val message = Messages("facto.successfully-added-user", name)
          Redirect(routes.Application.administration()).flashing("message" -> message)
      }
    )
  }

  def reactAppRoot = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.reactApp())
  }
  def reactApp(anyString: String) = reactAppRoot

  def reactAppWithoutCredentials = Action { implicit request =>
    Ok(views.html.reactApp())
  }

  def manualTests() = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.manualTests())
  }

  // ********** actions: JS files ********** //
  private lazy val localDatabaseWebWorkerResult: Result =
    Ok(s"""
          |importScripts("${Application.Assets.webworkerDeps}");
          |importScripts("${Application.Assets.factoAppClient}");
          |LocalDatabaseWebWorkerScript.run();
      """.stripMargin).as("application/javascript")
  def localDatabaseWebWorker = Action(_ => localDatabaseWebWorkerResult)

  private lazy val serviceWorkerResult: Result = {
    val jsFileTemplate = ResourceFiles.read("/serviceWorker.template.js")
    val scriptPathsJs = Application.Assets.all.map(asset => s"'$asset'").mkString(", ")
    val jsFileContent = jsFileTemplate.replace("$SCRIPT_PATHS_TO_CACHE$", scriptPathsJs)
    Ok(jsFileContent).as("application/javascript")
  }
  def serviceWorker = Action(_ => serviceWorkerResult)

  // ********** actions: Scala JS API backend ********** //
  def scalaJsApiPost(path: String) = AuthenticatedAction(parse.raw) { implicit user => implicit request =>
    val requestBuffer: ByteBuffer = request.body.asBytes(parse.UNLIMITED).get.asByteBuffer
    val argsMap = Unpickle[Map[String, ByteBuffer]].fromBytes(requestBuffer)

    val bytes = doScalaJsApiCall(path, argsMap)
    Ok(bytes)
  }

  def scalaJsApiGet(path: String) = AuthenticatedAction(parse.raw) { implicit user => implicit request =>
    val bytes = doScalaJsApiCall(path, argsMap = Map())
    Ok(bytes)
  }

  def scalaJsApiWebSocket = WebSocket.accept[Array[Byte], Array[Byte]] { request =>
    implicit val user = AuthenticatedAction.requireAuthenticatedUser(request)

    Flow[Array[Byte]].map { requestBytes =>
      val request = Unpickle[ScalaJsApiRequest].fromBytes(ByteBuffer.wrap(requestBytes))

      doScalaJsApiCall(request.path, request.args)
    }
  }

  // Note: This action manually implements what autowire normally does automatically. Unfortunately, autowire
  // doesn't seem to work for some reason.
  private def doScalaJsApiCall(path: String, argsMap: Map[String, ByteBuffer])(
      implicit user: User): Array[Byte] = {
    val scalaJsApiServer = scalaJsApiServerFactory.create()

    val responseBuffer = path match {
      case "getInitialData" =>
        Pickle.intoBytes(scalaJsApiServer.getInitialData())
      case "getAllEntities" =>
        val types = Unpickle[Seq[EntityType.any]].fromBytes(argsMap("types"))
        Pickle.intoBytes(scalaJsApiServer.getAllEntities(types))
      case "getEntityModifications" =>
        val updateToken = Unpickle[UpdateToken].fromBytes(argsMap("updateToken"))
        Pickle.intoBytes(scalaJsApiServer.getEntityModifications(updateToken))
      case "persistEntityModifications" =>
        val modifications = Unpickle[Seq[EntityModification]].fromBytes(argsMap("modifications"))
        Pickle.intoBytes(scalaJsApiServer.persistEntityModifications(modifications))
      case "executeDataQuery" =>
        val dbQuery = Unpickle[PicklableDbQuery].fromBytes(argsMap("dbQuery"))
        Pickle.intoBytes[Seq[Entity]](scalaJsApiServer.executeDataQuery(dbQuery))
      case "executeCountQuery" =>
        val dbQuery = Unpickle[PicklableDbQuery].fromBytes(argsMap("dbQuery"))
        Pickle.intoBytes(scalaJsApiServer.executeCountQuery(dbQuery))
    }

    val data: Array[Byte] = Array.ofDim[Byte](responseBuffer.remaining())
    responseBuffer.get(data)
    data
  }
}

object Application {
  private object Assets {
    private val factoAppProjectName: String = "client"
    private val webworkerDepsProjectName: String = "webworker-client-deps"

    val factoAppClient: String =
      scriptPathFromNames(s"$factoAppProjectName-opt.js", s"$factoAppProjectName-fastopt.js")
    val factoAppDeps: String =
      scriptPathFromNames(s"$factoAppProjectName-jsdeps.min.js", s"$factoAppProjectName-jsdeps.js")
    val webworkerDeps: String =
      scriptPathFromNames(s"$webworkerDepsProjectName-jsdeps.min.js", s"$webworkerDepsProjectName-jsdeps.js")

    val all: Seq[String] = Seq(
      factoAppClient,
      factoAppDeps,
      webworkerDeps,
      routes.WebJarAssets.at("metisMenu/1.1.3/metisMenu.min.css").path(),
      routes.WebJarAssets.at("font-awesome/4.6.2/css/font-awesome.min.css").path(),
      routes.WebJarAssets.at("font-awesome/4.6.2/fonts/fontawesome-webfont.woff2?v=4.6.2").path(),
      routes.WebJarAssets.at("font-awesome/4.6.2/fonts/fontawesome-webfont.woff?v=4.6.2 0").path(),
      routes.WebJarAssets.at("font-awesome/4.6.2/fonts/fontawesome-webfont.ttf?v=4.6.2").path(),
      routes.Assets.versioned("images/favicon192x192.png").path(),
      routes.Assets.versioned("lib/bootstrap/css/bootstrap.min.css").path(),
      routes.Assets.versioned("lib/fontello/css/fontello.css").path(),
      "/assets/lib/fontello/font/fontello.woff2?49985636",
      routes.Assets.versioned("bower_components/startbootstrap-sb-admin-2/dist/css/sb-admin-2.css").path(),
      routes.Assets.versioned("stylesheets/main.min.css").path(),
      routes.Assets.versioned("bower_components/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js").path(),
      routes.Application.localDatabaseWebWorker.path()
    )

    private def scriptPathFromNames(filenames: String*): String = {
      val filename =
        filenames
          .find(name => getClass.getResource(s"/public/$name") != null)
          .get
      routes.Assets.versioned(filename).path()
    }
  }

  object Forms {

    case class ChangePasswordData(loginName: String,
                                  oldPassword: String = "",
                                  password: String = "",
                                  passwordVerification: String = "")

    def changePasswordForm(implicit entityAccess: JvmEntityAccess) = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "oldPassword" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(ChangePasswordData.apply)(ChangePasswordData.unapply) verifying ("facto.error.old-password-is-incorrect", result =>
        result match {
          case ChangePasswordData(loginName, oldPassword, _, _) =>
            Users.authenticate(loginName, oldPassword)
      }) verifying ("facto.error.passwords-should-match", result =>
        result match {
          case ChangePasswordData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )

    case class AddUserData(loginName: String,
                           name: String = "",
                           password: String = "",
                           passwordVerification: String = "")

    val addUserForm = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "name" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(AddUserData.apply)(AddUserData.unapply) verifying ("facto.error.passwords-should-match", result =>
        result match {
          case AddUserData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )
  }
}
