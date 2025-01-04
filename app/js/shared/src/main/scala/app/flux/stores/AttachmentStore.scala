package app.flux.stores

import app.api.ScalaJsApiClient

import scala.concurrent.Future
import scala.scalajs.js.typedarray.ArrayBuffer

final class AttachmentStore(implicit
    scalaJsApiClient: ScalaJsApiClient,
) {
  def storeFileAndReturnHash(bytes: ArrayBuffer): Future[String] = {
    scalaJsApiClient.storeFileAndReturnHash(bytes)
  }
}
