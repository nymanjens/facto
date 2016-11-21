package controllers

import scala.collection.immutable.Seq
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.mvc._
import play.api.Logger
import models.accounting._
import scala.util.Random
import java.time.Instant
import common.testing.TestObjects._
import common.testing.TestUtils._
import models.accounting.config.{MoneyReservoir, Account}

// TODO: Get this to work.
class ApplicationFunctionalTest extends PlaySpecification {

  //  "Application" should {
  //
  //    "send 404 on a bad request" in new WithApplication {
  //      val result = route(FakeRequest(GET, "/boum")).get
  //      status(result) mustEqual NOT_FOUND
  //    }
  //
  //    "render the login page" in new WithApplication {
  //      val home = route(FakeRequest(GET, "/login/")).get
  //
  //      status(home) mustEqual OK
  //      contentType(home) must beSome.which(_ == "text/html")
  //      contentAsString(home) must contain ("Login")
  //      contentAsString(home) must contain ("Please Sign In")
  //    }
  //  }

  //  "index" in new WithApplication {
  //    val action: EssentialAction = Auth.login
  //
  //    val request = FakeRequest(Helpers.POST, controllers.routes.Auth.login().url, FakeHeaders(), """ {"name": "New Group", "collabs": ["foo", "asdf"]} """)
  //
  //
  //    val result = call(action, request)
  //
  //    status(result) mustEqual OK
  //    contentAsString(result) mustEqual "value"
  //  }

  //  "index try 2" in new WithApplication {
  //    val fakeRequest = FakeRequest(Helpers.POST, controllers.routes.Auth.login().url, FakeHeaders(), """ {"name": "New Group", "collabs": ["foo", "asdf"]} """)
  //
  //    val result = route(fakeRequest.withCookies(Cookie("myMemberId", "d"))).get
  //
  //    status(result) mustEqual OK
  //    Logger.info(contentAsString(result))
  //  }
}
