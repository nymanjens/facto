package models.user

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala2js.Converters._

final class JsUserManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[User]
    with User.Manager
