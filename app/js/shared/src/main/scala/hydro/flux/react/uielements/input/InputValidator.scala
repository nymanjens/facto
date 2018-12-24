package hydro.flux.react.uielements.input

trait InputValidator[Value] {
  def isValid(value: Value): Boolean
}

object InputValidator {
  def alwaysValid[Value]: InputValidator[Value] = _ => true
}
