package controllers

import scala.collection.immutable.Seq
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.mvc._
import models.accounting._
import scala.util.Random
import org.joda.time.DateTime
import common.testing.TestObjects._
import common.testing.TestUtils._
import models.accounting.config.{MoneyReservoir, Account}

// TODO: Get this to work.
class ApplicationFunctionalTest extends PlaySpecification {

  //  "index" in new WithApplication(fakeApplication) {
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

//  "index try 2" in new WithApplication(fakeApplication) {
//    val fakeRequest = FakeRequest(Helpers.POST, controllers.routes.Auth.login().url, FakeHeaders(), """ {"name": "New Group", "collabs": ["foo", "asdf"]} """)
//
//    val result = route(fakeRequest.withCookies(Cookie("myMemberId", "d"))).get
//
//    status(result) mustEqual OK
//    println(contentAsString(result))
//  }
}
