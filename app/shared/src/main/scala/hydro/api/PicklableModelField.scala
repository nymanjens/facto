package hydro.api

import app.models.access.ModelFields
import app.models.access.ModelField

/** Fork of ModelField that is picklable. */
case class PicklableModelField(fieldNumber: Int) {
  def toRegular: ModelField[_, _] = ModelFields.fromNumber(fieldNumber)
}

object PicklableModelField {
  def fromRegular(regular: ModelField[_, _]): PicklableModelField =
    PicklableModelField(ModelFields.toNumber(regular))
}
