package viewhelpers

object General {

  def ifElse[T](condition: Boolean)(ifClause: T)(elseClause: T): T = {
    if (condition) ifClause else elseClause
  }
}
