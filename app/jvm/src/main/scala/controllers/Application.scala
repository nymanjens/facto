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
import models.{Entity, SlickEntityAccess}
import akka.stream.scaladsl._
import models.access.DbQuery
import models.modification.EntityModification
import models.user.SlickUserManager
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._

import scala.collection.immutable.Seq

final class Application @Inject()(implicit override val messagesApi: MessagesApi,
                                  components: ControllerComponents,
                                  userManager: SlickUserManager,
                                  entityAccess: SlickEntityAccess,
                                  scalaJsApiServerFactory: ScalaJsApiServerFactory,
                                  playConfiguration: play.api.Configuration,
                                  env: play.api.Environment,
                                  webJarAssets: controllers.WebJarAssets)
    extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
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
            EntityModification.createUpdate(SlickUserManager.copyUserWithPassword(user, password)))
          val message = Messages("facto.successfully-updated-password")
          Redirect(routes.Application.profile()).flashing("message" -> message)
      }
    )
  }

  def administration() = AuthenticatedAction.requireAdminUser { implicit user => implicit request =>
    Ok(views.html.administration(users = userManager.fetchAllSync(), Forms.addUserForm))
  }

  def addUser() = AuthenticatedAction.requireAdminUser { implicit user => implicit request =>
    Forms.addUserForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.administration(users = userManager.fetchAllSync(), formWithErrors)), {
        case AddUserData(loginName, name, password, _) =>
          entityAccess.persistEntityModifications(
            EntityModification.createAddWithRandomId(SlickUserManager.createUser(loginName, password, name)))
          val message = Messages("facto.successfully-added-user", name)
          Redirect(routes.Application.administration()).flashing("message" -> message)
      }
    )
  }

  def reactAppRoot = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.reactApp())
  }
  def reactApp(anyString: String) = reactAppRoot

  // Note: This action manually implements what autowire normally does automatically. Unfortunately, autowire
  // doesn't seem to work for some reason.
  def scalaJsApi(path: String) = AuthenticatedAction(parse.raw) { implicit user => implicit request =>
    // Get the scalaJsApiServer
    val scalaJsApiServer = scalaJsApiServerFactory.create()

    // get the request body as ByteBuffer
    val requestBuffer: ByteBuffer = request.body.asBytes(parse.UNLIMITED).get.asByteBuffer
    val argsMap = Unpickle[Map[String, ByteBuffer]].fromBytes(requestBuffer)

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

    // Serialize response in HTTP response
    val data = Array.ofDim[Byte](responseBuffer.remaining())
    responseBuffer.get(data)
    Ok(data)
  }

  def scalaJsApiWebSocket = WebSocket.accept[Array[Byte], Array[Byte]] { request =>
    // Get the scalaJsApiServer
    val scalaJsApiServer = scalaJsApiServerFactory.create()

    // log the message to stdout and send response back to client
    Flow[Array[Byte]].map { requestBytes =>
      // get the request body as ByteBuffer
      val request = Unpickle[ScalaJsApiRequest].fromBytes(ByteBuffer.wrap(requestBytes))

      val responseBuffer = request.path match {
        case "getInitialData" =>
          Pickle.intoBytes(scalaJsApiServer.getInitialData())
        case "getAllEntities" =>
          val types = Unpickle[Seq[EntityType.any]].fromBytes(request.args("types"))
          Pickle.intoBytes(scalaJsApiServer.getAllEntities(types))
        case "getEntityModifications" =>
          val updateToken = Unpickle[UpdateToken].fromBytes(request.args("updateToken"))
          Pickle.intoBytes(scalaJsApiServer.getEntityModifications(updateToken))
        case "persistEntityModifications" =>
          val modifications = Unpickle[Seq[EntityModification]].fromBytes(request.args("modifications"))
          Pickle.intoBytes(scalaJsApiServer.persistEntityModifications(modifications))
        case "executeDataQuery" =>
          val dbQuery = Unpickle[PicklableDbQuery].fromBytes(request.args("dbQuery"))
          Pickle.intoBytes[Seq[Entity]](scalaJsApiServer.executeDataQuery(dbQuery))
        case "executeCountQuery" =>
          val dbQuery = Unpickle[PicklableDbQuery].fromBytes(request.args("dbQuery"))
          Pickle.intoBytes(scalaJsApiServer.executeCountQuery(dbQuery))
      }

      // Serialize response in HTTP response
      val data = Array.ofDim[Byte](responseBuffer.remaining())
      responseBuffer.get(data)
      data
    }
  }

  def manualTests() = AuthenticatedAction { implicit user => implicit request =>
    Ok(views.html.manualTests())
  }
}

object Application {
  // ********** forms ********** //
  object Forms {

    case class ChangePasswordData(loginName: String,
                                  oldPassword: String = "",
                                  password: String = "",
                                  passwordVerification: String = "")

    def changePasswordForm(implicit entityAccess: SlickEntityAccess) = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "oldPassword" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(ChangePasswordData.apply)(ChangePasswordData.unapply) verifying ("facto.error.old-password-is-incorrect", result =>
        result match {
          case ChangePasswordData(loginName, oldPassword, _, _) =>
            entityAccess.userManager.authenticate(loginName, oldPassword)
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
