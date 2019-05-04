package com.wanari.tutelar.core

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.ProviderApi._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class ProviderApiSpec extends TestBase with ScalatestRouteTest {

  val api = new ProviderApi {
    override val callbackConfig = ProviderApi.CallbackConfig("loginCallback=<<TOKEN>>", "error=<<ERROR>>")
    override def route(): Route = ???
  }

  "#completeLoginFlowWithRedirect" when {
    "successful token" in {
      Get() ~> api.completeLoginFlowWithRedirect(Future.successful("ToKeN")) ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("loginCallback=ToKeN")))
      }
    }

    "failed token" in {
      Get() ~> api.completeLoginFlowWithRedirect(Future.failed(new Exception())) ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("error=AUTHENTICATION_FAILED")))
      }
    }
  }

  "#completeLoginFlowWithJson" when {
    "successful token" in {
      Get() ~> api.completeLoginFlowWithJson(Future.successful("ToKeN")) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("ToKeN")
      }
    }

    "failed token" in {
      Get() ~> api.completeLoginFlowWithJson(Future.failed(new Exception())) ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }
}
