package spatutorial.client

import java.time.{LocalDate, LocalTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalField}

import org.scalajs.dom

//import spatutorial.client.components.GlobalStyles
import spatutorial.client.logger._

//import spatutorial.client.modules._
//import spatutorial.client.services.SPACircuit

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._

@JSExport("SPAMain")
object SPAMain extends js.JSApp {

  // Define the locations (pages) used in this application
  sealed trait Loc

  case object DashboardLoc extends Loc

  case object TodoLoc extends Loc

  //  // configure the router
  //  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
  //    import dsl._
  //
  //    val todoWrapper = SPACircuit.connect(_.todos)
  //    // wrap/connect components to the circuit
  //    (staticRoute(root, DashboardLoc) ~> renderR(ctl => SPACircuit.wrap(_.motd)(proxy => Dashboard(ctl, proxy)))
  //      | staticRoute("#todo", TodoLoc) ~> renderR(ctl => todoWrapper(Todo(_)))
  //      ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
  //  }.renderWith(layout)
  //
  //  val todoCountWrapper = SPACircuit.connect(_.todos.map(_.items.count(!_.completed)).toOption)
  //  // base layout for all pages
  //  def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
  //    <.div(
  //      // here we use plain Bootstrap class names as these are specific to the top level layout defined here
  //      <.nav(^.className := "navbar navbar-inverse navbar-fixed-top",
  //        <.div(^.className := "container",
  //          <.div(^.className := "navbar-header", <.span(^.className := "navbar-brand", "SPA Tutorial")),
  //          <.div(^.className := "collapse navbar-collapse",
  //            // connect menu to model, because it needs to update when the number of open todos changes
  //            todoCountWrapper(proxy => MainMenu(c, r.page, proxy))
  //          )
  //        )
  //      ),
  //      // currently active module is shown in this container
  //      <.div(^.className := "container", r.render())
  //    )
  //  }

  @JSExport
  def main(): Unit = {
    //     log.warn("Application starting")
    //    // send log messages also to the server
    //    log.enableServerLogging("/logging")
    //    log.info("This message goes to server as well")
    //
    //    // create stylesheet
    //    GlobalStyles.addToDocument()
    //    // create the router
    //    val router = Router(BaseUrl.until_#, routerConfig)
    //    // tell React to render the router in the document body
    //    ReactDOM.render(router(), dom.document.getElementById("root"))
  }

  import common._
  import common.time._
  import common.time.JavaTimeImplicits._
  import java.time.Month._
  import java.time.LocalDate
  import java.time.format._

  @JSExport
  def factoPortabilityTest(): Unit = {
    out("factoPortabilityTest() starting")

    val clock = new JvmClock
    out(clock.now)

    out(LocalDate.now)
    out(LocalTime.now)
    val time = LocalTime.now

//    TimeUtils.requireStartOfMonth(LocalDate.of(2010, NOVEMBER, 1))
//    out(TimeUtils.allMonths)
//    out(TimeUtils.instantAt(2100, DECEMBER, 1) < TimeUtils.instantAt(2100, DECEMBER, 2))
//    out(DatedMonth.containing(TimeUtils.instantAt(2100, DECEMBER, 1)).contains(TimeUtils.instantAt(2100, DECEMBER, 10)))
//    out(DatedMonth.allMonthsIn(2012))
//
//    val instant = TimeUtils.instantAt(2012, DECEMBER, 1)
//    implicit val i18n = new I18n {
//      override def apply(key: String, args: Any*): String = key
//    }
//    out(Formatting.formatDateTime(instant))
//    out(Formatting.formatDate(instant))
//
//    out(MonthRange.forYear(2012) intersection MonthRange.atLeast(DatedMonth.containing(instant)))
//
//    X(null)
  }

  def out(x: Any): Unit = {
    println(x)
  }

  case class X(s: String) {
    //    Require.requireNonNullFields(this)
    //    Require.requireNonNull(s)
  }
}
