package api

import models.accounting.config.Config

trait Api {
  def getAccountingConfig(): Config
}
