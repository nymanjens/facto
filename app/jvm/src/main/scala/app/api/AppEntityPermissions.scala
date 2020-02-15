package app.api

import app.models.money.ExchangeRateMeasurement
import app.models.user.User
import hydro.api.EntityPermissions
import hydro.models.modification.EntityModification
import hydro.models.Entity

final class AppEntityPermissions extends EntityPermissions {

  override def checkAllowedForWrite(modification: EntityModification)(implicit user: User): Unit = {
    EntityPermissions.DefaultImpl.checkAllowedForWrite(modification)

    require(
      modification.entityType != ExchangeRateMeasurement.Type,
      "Client initiated exchange rate measurement changes are not allowed")
  }

  override def isAllowedToRead(entity: Entity)(implicit user: User): Boolean = {
    EntityPermissions.DefaultImpl.isAllowedToRead(entity)
  }
}
