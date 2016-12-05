package api

import com.google.inject.AbstractModule

final class ApiModule extends AbstractModule {
  override def configure() = {
    bind(classOf[Api]).to(classOf[ApiImpl])
  }
}
