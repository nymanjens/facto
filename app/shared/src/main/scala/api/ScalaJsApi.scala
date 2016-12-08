package api

import models.accounting.config.Config

trait ScalaJsApi {
  def getAccountingConfig(): Config

  def welcomeMsg(name: String): String
}
