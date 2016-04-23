package models.activeslick

class ActiveSlickException(msg: String) extends RuntimeException(msg)

case class ManyRowsAffectedException(affectedRecordsCount: Int)
  extends ActiveSlickException(s"Expected single row affected, got $affectedRecordsCount instead")

case object NoRowsAffectedException extends ActiveSlickException("No rows affected")

case class RowNotFoundException[T](notFoundRecord: T)
  extends ActiveSlickException(s"Row not found: $notFoundRecord")
