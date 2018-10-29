package com.wanari.tutelar.github

import akka.http.scaladsl.model.{StatusCodes, Uri}
import com.wanari.tutelar.RouteTestBase
import org.mockito.Mockito._

import scala.concurrent.Future

class GithubItSpec extends RouteTestBase {

  "GET /github/login" should {
    "return forward" in new BaseTestScope {
      when(services.githubService.generateIdentifierUrl) thenReturn Future.successful(Uri.Empty)
      Get("/github/login") ~> route ~> check {
        status shouldEqual StatusCodes.Found
      }
    }
    "return with 401 on bad config" in new BaseTestScope {
      when(services.githubService.generateIdentifierUrl) thenReturn Future.failed(new Exception())
      Get("/github/login") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
  }
  "GET /github/callback" should {
    val map = Map("a" -> "as", "b" -> "bs")
    "return OK" in new BaseTestScope {
      when(services.githubService.authenticateWithCallback(map)) thenReturn Future.successful({})
      Get("/github/callback?a=as&b=bs") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "return with 401 on error" in new BaseTestScope {
      when(services.githubService.authenticateWithCallback(map)) thenReturn Future.failed(new Exception())
      Get("/github/callback?a=as&b=bs") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
  }
}
