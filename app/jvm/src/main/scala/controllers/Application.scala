package controllers

import java.net.URL
import java.nio.ByteBuffer

import akka.stream.scaladsl._
import api.Picklers._
import api.ScalaJsApi.UpdateToken
import api.{PicklableDbQuery, ScalaJsApiRequest, ScalaJsApiServerFactory}
import boopickle.Default._
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.io.Resources
import com.google.inject.Inject
import common.GuavaReplacement.Splitter
import common.ResourceFiles
import controllers.Application.Forms
import controllers.Application.Forms.{AddUserData, ChangePasswordData}
import controllers.helpers.AuthenticatedAction
import models.Entity
import models.access.JvmEntityAccess
import models.modification.{EntityModification, EntityType}
import models.user.{User, Users}
import play.api.Mode
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
  private lazy val localDatabaseWebWorkerResultCache: Result =
    Ok(s"""
          |importScripts("${Application.Assets.webworkerDeps.urlPath}");
          |importScripts("${Application.Assets.factoAppClient.urlPath}");
          |LocalDatabaseWebWorkerScript.run();
      """.stripMargin).as("application/javascript")
  def localDatabaseWebWorker = Action(_ => localDatabaseWebWorkerResultCache)

  private def serviceWorkerResultFunc(): Result = {
    val jsFileTemplate = ResourceFiles.read("/serviceWorker.template.js")
    val scriptPathsJs = Application.Assets.all.map(asset => s"'${asset.urlPath}'").mkString(", ")
    val cacheNameSuffix = {
      val hasher = Hashing.murmur3_128().newHasher()
      for (asset <- Application.Assets.all) {
        hasher.putString(asset.urlPath, Charsets.UTF_8)
        for (resource <- asset.maybeLocalResource) {
          hasher.putBytes(Resources.toByteArray(resource))
        }
      }
      hasher.hash().toString
    }
    val jsFileContent = jsFileTemplate
      .replace("$SCRIPT_PATHS_TO_CACHE$", scriptPathsJs)
      .replace("$CACHE_NAME_SUFFIX$", cacheNameSuffix)
    Ok(jsFileContent).as("application/javascript").withHeaders("Cache-Control" -> "no-cache")
  }
  private lazy val serviceWorkerResultCache: Result = serviceWorkerResultFunc()
  def serviceWorker =
    Action(_ => if (env.mode == Mode.Dev) serviceWorkerResultFunc() else serviceWorkerResultCache)

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

    val factoAppClient: Asset =
      firstExistingVersionedAsset(s"$factoAppProjectName-opt.js", s"$factoAppProjectName-fastopt.js")
    val factoAppDeps: Asset =
      firstExistingVersionedAsset(s"$factoAppProjectName-jsdeps.min.js", s"$factoAppProjectName-jsdeps.js")
    val webworkerDeps: Asset =
      firstExistingVersionedAsset(
        s"$webworkerDepsProjectName-jsdeps.min.js",
        s"$webworkerDepsProjectName-jsdeps.js")

    val all: Seq[Asset] = Seq(
      factoAppClient,
      factoAppDeps,
      webworkerDeps,
      WebJarAsset("metisMenu/1.1.3/metisMenu.min.css"),
      WebJarAsset("font-awesome/4.6.2/css/font-awesome.min.css"),
      WebJarAsset("font-awesome/4.6.2/fonts/fontawesome-webfont.woff2?v=4.6.2"),
      WebJarAsset("font-awesome/4.6.2/fonts/fontawesome-webfont.woff?v=4.6.2 0"),
      WebJarAsset("font-awesome/4.6.2/fonts/fontawesome-webfont.ttf?v=4.6.2"),
      VersionedAsset("images/favicon192x192.png"),
      VersionedAsset("lib/bootstrap/css/bootstrap.min.css"),
      VersionedAsset("lib/fontello/css/fontello.css"),
      UnversionedAsset("lib/fontello/font/fontello.woff2?49985636"),
      VersionedAsset("bower_components/startbootstrap-sb-admin-2/dist/css/sb-admin-2.css"),
      VersionedAsset("stylesheets/main.min.css"),
      VersionedAsset("bower_components/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js"),
      DynamicAsset(routes.Application.localDatabaseWebWorker),
    )

    private def firstExistingVersionedAsset(filenames: String*): Asset =
      VersionedAsset(filenames.find(name => ResourceFiles.exists(s"/public/$name")).get)

    sealed trait Asset {
      def maybeLocalResource: Option[URL]
      def urlPath: String
    }
    abstract class ResourceAsset(relativePath: String) extends Asset {
      // Remove the query parameters to find the local resource path
      private val resourcePath: String = Splitter.on('?').split(s"/public/$relativePath").head
      require(ResourceFiles.exists(resourcePath), s"Could not find asset at $resourcePath")

      override final def maybeLocalResource = Some(getClass.getResource(resourcePath))
    }
    case class VersionedAsset(relativePath: String) extends ResourceAsset(relativePath) {
      override def urlPath = routes.Assets.versioned(relativePath).path()
    }
    case class UnversionedAsset(relativePath: String) extends ResourceAsset(relativePath) {
      override def urlPath = s"/assets/$relativePath"
    }
    case class WebJarAsset(relativePath: String) extends Asset {
      override def maybeLocalResource = None
      override def urlPath = routes.WebJarAssets.at(relativePath).path()
    }
    case class DynamicAsset(call: Call) extends Asset {
      override def maybeLocalResource = None
      override def urlPath = call.path()
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
