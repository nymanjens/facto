package api

import com.google.inject._
import models.accounting.config.Config
import api.ScalaJsApi.AllEntries
import models.EntityAccess

private[api] final class ScalaJsApiServer @Inject()(implicit accountingConfig: Config,
                                                    entityAccess: EntityAccess) extends ScalaJsApi {

  override def getAccountingConfig(): Config = accountingConfig

  override def getAllEntities(): AllEntries = AllEntries(
    users = entityAccess.userManager.fetchAll(),
    transactions = entityAccess.transactionManager.fetchAll(),
    transactionGroups = entityAccess.transactionGroupManager.fetchAll(),
    balanceChecks = entityAccess.balanceCheckManager.fetchAll(),
    exchangeRateMeasurements = entityAccess.exchangeRateMeasurementManager.fetchAll()
  )
}
