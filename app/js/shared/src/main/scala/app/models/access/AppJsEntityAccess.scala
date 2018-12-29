package app.models.access

import app.models.user.User

trait AppJsEntityAccess extends JsEntityAccess {

  override def newQuerySyncForUser(): DbResultSet.Sync[User]
}
