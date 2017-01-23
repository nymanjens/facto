package common.testing

import flux.Dispatcher
import models.accounting.Transaction
import models.accounting.JsTransactionManager

object TestModule {

  import com.softwaremill.macwire._

  implicit lazy val fakeRemoteDataProxy = wire[FakeRemoteDatabaseProxy]
  implicit lazy val fakeClock = wire[FakeClock]
  implicit lazy val dispatcher = wire[Dispatcher.FakeSynchronous]
}
