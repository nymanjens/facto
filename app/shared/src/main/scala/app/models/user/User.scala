package app.models.user

import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime

case class User(
    loginName: String,
    passwordHash: String,
    name: String,
    isAdmin: Boolean,
    expandCashFlowTablesByDefault: Boolean,
    expandLiquidationTablesByDefault: Boolean,
    override val idOption: Option[Long] = None,
    override val lastUpdateTime: LastUpdateTime = LastUpdateTime.neverUpdated,
) extends UpdatableEntity {

  override def withId(id: Long) = copy(idOption = Some(id))
  override def withLastUpdateTime(time: LastUpdateTime): Entity = copy(lastUpdateTime = time)
}

object User {
  implicit val Type: EntityType[User] = EntityType()

  def tupled = (this.apply _).tupled
}
