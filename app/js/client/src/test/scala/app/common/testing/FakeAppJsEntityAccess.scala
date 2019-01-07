package app.common.testing

import app.models.access._
import app.models.user.User
import hydro.common.testing.FakeJsEntityAccess
import hydro.models.access.DbResultSet

final class FakeAppJsEntityAccess extends FakeJsEntityAccess with AppJsEntityAccess {

  override def newQuerySyncForUser() = {
    DbResultSet.fromExecutor(queryExecutor[User])
  }
}
