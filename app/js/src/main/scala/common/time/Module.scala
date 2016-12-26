package common.time

object Module {

  import com.softwaremill.macwire._

  implicit lazy val clock: Clock = wire[JsClock]
}