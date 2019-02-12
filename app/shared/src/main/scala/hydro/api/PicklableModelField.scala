package hydro.api

import app.models.access.ModelFields
import hydro.models.access.ModelField

/** Fork of ModelField that is picklable. */
case class PicklableModelField(fieldNumber: Int) {
  def toRegular: ModelField.any = ModelFields.fromNumber(fieldNumber)
}

object PicklableModelField {
  def fromRegular(regular: ModelField.any): PicklableModelField =
    PicklableModelField(ModelFields.toNumber(regular))
}
