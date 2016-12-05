package api

import com.google.inject._
import models.accounting.config.Config

private[api] final class ApiImpl @Inject() (implicit accountingConfig: Config) extends Api {
  override def getAccountingConfig(): Config = accountingConfig
}
