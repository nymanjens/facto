package api

import models.access.ModelField

case class PicklableModelField(fieldNumber: Int) {
  def toRegular: ModelField[_, _] = ModelField.fromNumber(fieldNumber)
}

object PicklableModelField {
  def fromRegular(regular: ModelField[_, _]): PicklableModelField =
    PicklableModelField(ModelField.toNumber(regular))
}
