package app.api

import models.access.ModelField

/** Fork of ModelField that is picklable. */
case class PicklableModelField(fieldNumber: Int) {
  def toRegular: ModelField[_, _] = ModelField.fromNumber(fieldNumber)
}

object PicklableModelField {
  def fromRegular(regular: ModelField[_, _]): PicklableModelField =
    PicklableModelField(ModelField.toNumber(regular))
}
