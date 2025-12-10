package app.controllers

import hydro.controllers.InternalApi.ScalaJsApiCaller
import app.controllers.helpers.ScalaJsApiCallerImpl
import com.google.inject.AbstractModule

import scala.collection.immutable.Seq

final class ControllersModule extends AbstractModule {

  override def configure() = {
    bind(classOf[ScalaJsApiCaller]).to(classOf[ScalaJsApiCallerImpl])
  }
}
