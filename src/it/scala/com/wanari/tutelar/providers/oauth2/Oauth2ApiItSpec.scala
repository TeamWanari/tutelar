package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.MissingQueryParamRejection
import cats.MonadError
import com.wanari.tutelar.RouteTestBase
import org.mockito.Mockito._
import org.mockito.ArgumentMatchersSugar._

import scala.concurrent.Future

class Oauth2ApiItSpec extends RouteTestBase {

  Seq("facebook", "github", "google").foreach { provider =>
    s"GET /$provider/login" should {
      "return forward" in new BaseTestScope {
        when(services.getOauthServiceByName(provider).generateIdentifierUrl(any[MonadError[Future, Throwable]])) thenReturn Future
          .successful(Uri.Empty)
        Get(s"/$provider/login") ~> route ~> check {
          status shouldEqual StatusCodes.Found
        }
      }
      "return with 401 on bad config" in new BaseTestScope {
        when(services.getOauthServiceByName(provider).generateIdentifierUrl(any[MonadError[Future, Throwable]])) thenReturn Future
          .failed(new Exception())
        Get(s"/$provider/login") ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
    }
    s"GET /$provider/callback" should {
      "return OK" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .authenticateWithCallback(eqTo("a"), eqTo("b"))(any[MonadError[Future, Throwable]])
        ) thenReturn Future.successful("asd")
        Get(s"/$provider/callback?code=a&state=b") ~> route ~> check {
          status shouldEqual StatusCodes.Found
        }
      }
      "return with 401 on error" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .authenticateWithCallback(eqTo("a"), eqTo("b"))(any[MonadError[Future, Throwable]])
        ) thenReturn Future.failed(new Exception())
        Get(s"/$provider/callback?code=a&state=b") ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
      "rejects on bad/missing query parameters" in new BaseTestScope {
        Get(s"/$provider/callback?code=a") ~> route ~> check {
          rejection shouldEqual MissingQueryParamRejection("state")
        }
      }
    }
  }
}
