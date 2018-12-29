package app.models.access

import app.models.user.User

/** Central point of access to the storage layer. */
trait AppEntityAccess extends EntityAccess {

  def newQuerySyncForUser(): DbResultSet.Sync[User]
}
