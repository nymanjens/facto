package hydro.models.access

import hydro.models.modification.EntityModification

trait PendingModificationsListener {
  def onPendingModificationAddedByOtherInstance(modification: EntityModification): Unit
  def onPendingModificationRemovedByOtherInstance(modificationPseudoUniqueIdentifier: Long): Unit
}
