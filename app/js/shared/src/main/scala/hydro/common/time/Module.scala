package hydro.common.time

final class Module {

  implicit lazy val clock: Clock = new JsClock
}
