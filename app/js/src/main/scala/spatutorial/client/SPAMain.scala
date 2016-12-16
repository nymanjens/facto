package spatutorial.client

import scala.collection.mutable
import java.time.{DayOfWeek, LocalDate, LocalTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalField}

import api.ScalaJsApiClient
import models.accounting.Transaction
import org.scalajs.dom

//import spatutorial.client.components.GlobalStyles
import spatutorial.client.logger._

//import spatutorial.client.modules._
//import spatutorial.client.services.SPACircuit

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.annotation.meta.field
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scala.concurrent.ExecutionContext.Implicits.global

import jsfacades._
import scala2js.Scala2Js
import scala2js.Converters._

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
  import common.time.LocalDateTime
  import java.time.format._

  @JSExport
  def factoPortabilityTest(): Unit = {
    out("factoPortabilityTest() starting")

    implicit val clock = new JsClock
    val date = LocalDateTime.of(2012, DECEMBER, 1, 0, 0)

    out(clock.now)
    out(date)
    TimeUtils.requireStartOfMonth(LocalDate.of(2010, NOVEMBER, 1))
    out(TimeUtils.allMonths)
    out(DatedMonth.containing(LocalDateTime.of(2100, DECEMBER, 1, 0, 0)).contains(LocalDateTime.of(2100, DECEMBER, 10, 0, 0)))
    out(DatedMonth.allMonthsIn(2012))
    out(MonthRange.forYear(2012) intersection MonthRange.atLeast(DatedMonth.containing(date)))

    implicit val i18n = new I18n {
      override def apply(key: String, args: Any*): String = key
    }
    out(Formatting.formatDateTime(date))
    out(Formatting.formatDate(date))
    out(Formatting.formatDateTime(clock.now))
    out(Formatting.formatDate(clock.now))

    try {
      X(null)
      throw new AssertionError()
    } catch {
      case e: IllegalArgumentException =>
    }
    X("abc")

    val transaction = Scala2Js.toScala[Transaction](js.JSON.parse(
      """{
          "transactionGroupId": 1232138,
          "issuerId": 32978,
          "beneficiaryAccountCode": "abc",
          "moneyReservoirCode": "def",
          "categoryCode": "ghi",
          "description": "safd",
          "flowInCents": "8837432",
          "detailDescription": "jkfdsa",
          "tagsString": "",
          "createdDate": 1237897,
          "transactionDate": 1481666404,
          "consumedDate": 1481666404,
          "id": 43334
        }
      """))
    out(transaction)
    val transactionJs = Scala2Js.toJs(transaction)
    outJs(transactionJs)
    out(Scala2Js.toScala[Transaction](transactionJs))

    //    new ScalaJsApiClient().getAccountingConfig().foreach(out)
    //    new ScalaJsApiClient().getAllEntities().foreach(out)


    val db = Loki.Database.persistent("loki-in-scalajs-test")
    def save(callback: () => Unit = () => {}) = {
      val transactions = db.getOrAddCollection("transactions")
      new ScalaJsApiClient().getAllEntities().foreach(allEntries => {
        for (transaction <- allEntries.transactions) {
          transactions.insert(Scala2Js.toJs(transaction))
        }
        db.saveDatabase(() => {
          out("Done saving transactions")
          callback()
        })
      })
    }
    def load() = {
      out("loading")
      db.loadDatabase()
      db.loadDatabase(() => {
        out("loaded")
        val children = db.getOrAddCollection("transactions")
        out(children.find(js.Dictionary("categoryCode" -> "MED")).toSeq map (Scala2Js.toScala[Transaction]))
      })
    }
//    save(() => load())
        load()
  }

  def outJs(x: js.Any): Unit = {
    out(js.JSON.stringify(x))
  }

  def out(x: Any): Unit = {
    println(x)
  }

  case class X(s: String) {
    Require.requireNonNull(s)
  }
}
