package viewhelpers

import play.twirl.api.Html

object General {

  def ifElse[T](condition: Boolean)(ifClause: T)(elseClause: T): T = {
    if (condition) ifClause else elseClause
  }
}
