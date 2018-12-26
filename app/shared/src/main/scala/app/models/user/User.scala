package app.models.user

import app.models.Entity

case class User(loginName: String,
                passwordHash: String,
                name: String,
                isAdmin: Boolean,
                expandCashFlowTablesByDefault: Boolean,
                expandLiquidationTablesByDefault: Boolean,
                idOption: Option[Long] = None)
    extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))
}

object User {
  def tupled = (this.apply _).tupled
}
