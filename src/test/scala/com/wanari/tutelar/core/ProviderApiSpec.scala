package com.wanari.tutelar.core

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.wanari.tutelar.TestBase

import scala.concurrent.Future

class ProviderApiSpec extends TestBase with ScalatestRouteTest {

  val api = new ProviderApi {
    override val callbackConfig = () => {
      Future.successful(
        ProviderApi.CallbackConfig("loginCallback=<<TOKEN>>", "errorCallback")
      )
    }
    override def route(): Route = ???
  }

  "#completeLoginFlow" when {
    "successful token" in {
      Get() ~> api.completeLoginFlow(Future.successful("ToKeN")) ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("loginCallback=ToKeN")))
      }
    }

    "failed token" in {
      Get() ~> api.completeLoginFlow(Future.failed(new Exception())) ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("errorCallback")))
      }
    }
  }
}
