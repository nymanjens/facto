package hydro.controllers

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import app.api.ScalaJsApi.HydroPushSocketPacket
import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken
import app.api.ScalaJsApi.UpdateToken
import app.api.ScalaJsApiServerFactory
import app.models.access.JvmEntityAccess
import app.models.user.User
import boopickle.Default._
import app.api.Picklers._
import app.AppVersion
import com.google.inject.Inject
import com.google.inject.Singleton
import hydro.api.ScalaJsApiRequest
import hydro.common.UpdateTokens.toInstant
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.publisher.Publishers
import hydro.common.publisher.TriggerablePublisher
import hydro.common.time.Clock
import hydro.controllers.InternalApi.ScalaJsApiCaller
import hydro.controllers.helpers.AuthenticatedAction
import hydro.controllers.InternalApi.HydroPushSocketHeartbeatScheduler
import hydro.models.modification.EntityModificationEntity
import hydro.models.slick.SlickUtils.dbApi._
import hydro.models.slick.SlickUtils.dbRun
import hydro.models.slick.SlickUtils.instantToSqlTimestampMapper
import hydro.models.slick.StandardSlickEntityTableDefs.EntityModificationEntityDef
import org.reactivestreams.Publisher
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

final class InternalApi @Inject()(
    implicit override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    scalaJsApiServerFactory: ScalaJsApiServerFactory,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
    scalaJsApiCaller: ScalaJsApiCaller,
    hydroPushSocketHeartbeatScheduler: HydroPushSocketHeartbeatScheduler,
) extends AbstractController(components)
    with I18nSupport {

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

  def scalaJsApiWebsocket = WebSocket.accept[Array[Byte], Array[Byte]] { request =>
    implicit val user = AuthenticatedAction.requireAuthenticatedUser(request)

    Flow[Array[Byte]].map { requestBytes =>
      val request = Unpickle[ScalaJsApiRequest].fromBytes(ByteBuffer.wrap(requestBytes))

      doScalaJsApiCall(request.path, request.args)
    }
  }

  def hydroPushSocketWebsocket(updateToken: UpdateToken) = WebSocket.accept[Array[Byte], Array[Byte]] {
    request =>
      AuthenticatedAction.requireAuthenticatedUser(request)

      def packetToBytes(packet: HydroPushSocketPacket): Array[Byte] = {
        val responseBuffer = Pickle.intoBytes(packet)
        val data: Array[Byte] = Array.ofDim[Byte](responseBuffer.remaining())
        responseBuffer.get(data)
        data
      }

      // Start recording all updates
      val entityModificationPublisher =
        Publishers.delayMessagesUntilFirstSubscriber(entityAccess.entityModificationPublisher)

      // Calculate updates from the update token onwards
      val firstModificationsWithToken = {
        // All modifications are idempotent so we can use the time when we started getting the entities as next
        // update token.
        val nextUpdateToken: UpdateToken = toUpdateToken(clock.nowInstant)

        val modifications = {
          val modificationEntities = dbRun(
            entityAccess
              .newSlickQuery[EntityModificationEntity]()
              .filter(_.instant >= toInstant(updateToken))
              .sortBy(m => (m.instant, m.instantNanos)))
          modificationEntities.toStream.map(_.modification).toVector
        }

        EntityModificationsWithToken(modifications, nextUpdateToken)
      }
      val versionCheck = HydroPushSocketPacket.VersionCheck(versionString = AppVersion.versionString)

      val in = Sink.ignore
      val out =
        Source.single(packetToBytes(firstModificationsWithToken)) concat
          Source.single(packetToBytes(versionCheck)) concat
          Source.fromPublisher(
            Publishers
              .map(
                Publishers.combine[HydroPushSocketPacket](
                  entityModificationPublisher,
                  hydroPushSocketHeartbeatScheduler.publisher),
                packetToBytes))
      Flow.fromSinkAndSource(in, out)
  }

  // Note: This action manually implements what autowire normally does automatically. Unfortunately, autowire
  // doesn't seem to work for some reason.
  private def doScalaJsApiCall(path: String, argsMap: Map[String, ByteBuffer])(
      implicit user: User): Array[Byte] = {
    val responseBuffer = scalaJsApiCaller(path, argsMap)

    val data: Array[Byte] = Array.ofDim[Byte](responseBuffer.remaining())
    responseBuffer.get(data)
    data
  }
}
object InternalApi {
  trait ScalaJsApiCaller {
    def apply(path: String, argsMap: Map[String, ByteBuffer])(implicit user: User): ByteBuffer
  }

  @Singleton
  private[controllers] class HydroPushSocketHeartbeatScheduler @Inject()(
      implicit actorSystem: ActorSystem,
      executionContext: ExecutionContext,
  ) {

    private val publisher_ : TriggerablePublisher[HydroPushSocketPacket.Heartbeat.type] =
      new TriggerablePublisher()

    actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = 5.seconds) {
      publisher_.trigger(HydroPushSocketPacket.Heartbeat)
    }

    def publisher: Publisher[HydroPushSocketPacket.Heartbeat.type] = publisher_
  }
}
