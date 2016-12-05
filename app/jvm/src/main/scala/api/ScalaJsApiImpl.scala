package api

import com.google.inject._
import models.accounting.config.Config

private[api] final class ScalaJsApiImpl @Inject() (implicit accountingConfig: Config) extends ScalaJsApi {
  override def getAccountingConfig(): Config = accountingConfig
}
