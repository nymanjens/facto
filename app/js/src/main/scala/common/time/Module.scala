package common.time

final class Module {

  import com.softwaremill.macwire._

  implicit lazy val clock: Clock = wire[JsClock]
}
