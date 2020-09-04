package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.EitherT
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.AuthenticationFailed
import com.wanari.tutelar.core.ProviderApi._

class ProviderApiSpec extends TestBase with ScalatestRouteTest {

  val api = new ProviderApi {
    override val callbackConfig =
      ProviderApi.CallbackConfig("loginCallback=<<TOKEN>>|<<REFRESH_TOKEN>>", "error=<<ERROR>>")
    override def route(): Route = ???
  }

  "#completeLoginFlowWithRedirect" when {
    "successful token" in {
      Get() ~> api.completeLoginFlowWithRedirect(EitherT.rightT(TokenData("ToKeN", "ReFrEsH"))) ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("loginCallback=ToKeN|ReFrEsH")))
      }
    }

    "failed token" in {
      Get() ~> api.completeLoginFlowWithRedirect(EitherT.leftT(AuthenticationFailed())) ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("error=AUTHENTICATION_FAILED")))
      }
    }
  }

  "#completeLoginFlowWithJson" when {
    "successful token" in {
      Get() ~> api.completeLoginFlowWithJson(EitherT.rightT(TokenData("ToKeN", "ReFrEsH"))) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("ToKeN", "ReFrEsH")
      }
    }

    "failed token" in {
      Get() ~> api.completeLoginFlowWithJson(EitherT.leftT(AuthenticationFailed())) ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }
}
