package flux.action

object Module {

  import com.softwaremill.macwire._

  implicit val dispatcher: Dispatcher = wire[Dispatcher.Impl]
}
