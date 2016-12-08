package api

import com.google.inject._
import models.accounting.config.Config

private[api] final class ScalaJsApiServer @Inject() (implicit accountingConfig: Config) extends ScalaJsApi {
  override def getAccountingConfig(): Config = accountingConfig

  override def welcomeMsg(name: String): String =
    s"Welcome to SPA, $name!"
}
