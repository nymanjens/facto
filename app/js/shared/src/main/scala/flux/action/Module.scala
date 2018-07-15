package flux.action

final class Module {

  implicit val dispatcher: Dispatcher = new Dispatcher.Impl
}
