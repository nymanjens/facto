package app.models.access

import app.models.user.User
import hydro.models.access.DbResultSet
import hydro.models.access.EntityAccess

/** Central point of access to the storage layer. */
trait AppEntityAccess extends EntityAccess {

  def newQuerySyncForUser(): DbResultSet.Sync[User]
}
