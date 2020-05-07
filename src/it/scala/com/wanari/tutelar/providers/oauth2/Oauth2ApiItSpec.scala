package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, Location}
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, MissingQueryParamRejection}
import cats.MonadError
import cats.data.EitherT
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors._
import com.wanari.tutelar.providers.oauth2.OAuth2Api._
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.mockito.ArgumentMatchersSugar._

import scala.concurrent.Future

class Oauth2ApiItSpec extends RouteTestBase {
  import cats.instances.future._

  Seq("facebook", "github", "google", "microsoft").foreach { provider =>
    s"GET /$provider/login" should {
      "return forward" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .generateIdentifierUrl(any[Option[LongTermToken]])(any[MonadError[Future, Throwable]])
        ) thenReturn Future
          .successful(Uri.Empty)
        Get(s"/$provider/login") ~> route ~> check {
          status shouldEqual StatusCodes.Found
        }
      }
      "return with 401 on bad config" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .generateIdentifierUrl(any[Option[LongTermToken]])(any[MonadError[Future, Throwable]])
        ) thenReturn Future
          .failed(new Exception())
        Get(s"/$provider/login") ~> route ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
    }
    s"GET /$provider/callback" should {
      "return redirect with callback" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .authenticateWithCallback(eqTo("a"), eqTo("b"))(any[MonadError[Future, Throwable]], any[LogContext])
        ) thenReturn EitherT.rightT(TokenData("asd", "longasd"))
        Get(s"/$provider/callback?code=a&state=b") ~> route ~> check {
          status shouldEqual StatusCodes.Found
          headers should contain(Location(Uri("https://lvh.me:9443/index.html?token=asd&refresh_token=longasd")))
        }
      }
      "return redirect with error" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .authenticateWithCallback(eqTo("a"), eqTo("b"))(any[MonadError[Future, Throwable]], any[LogContext])
        ) thenReturn EitherT.leftT(AuthenticationFailed())
        Get(s"/$provider/callback?code=a&state=b") ~> route ~> check {
          status shouldEqual StatusCodes.Found
          headers should contain(Location(Uri("https://lvh.me:9443/index.html?error=AUTHENTICATION_FAILED")))
        }
      }
      "rejects on bad/missing query parameters" in new BaseTestScope {
        Get(s"/$provider/callback?code=a") ~> route ~> check {
          rejection shouldEqual MissingQueryParamRejection("state")
        }
      }
    }
    s"GET /$provider/token" should {
      "return access token" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .getAccessTokenForUser(any[String])(any[MonadError[Future, Throwable]], any[LogContext])
        ) thenReturn EitherT.rightT(AccessToken("token", None))
        Get(s"/$provider/token?userId=asd") ~> addCredentials(
          BasicHttpCredentials("testuser", "testpass")
        ) ~> route ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[AccessToken] shouldBe AccessToken("token", None)
        }
        verify(services.getOauthServiceByName(provider), times(1))
          .getAccessTokenForUser(eqTo("asd"))(any[MonadError[Future, Throwable]], any[LogContext])
      }
      "return NotFound on AppError" in new BaseTestScope {
        when(
          services
            .getOauthServiceByName(provider)
            .getAccessTokenForUser(any[String])(any[MonadError[Future, Throwable]], any[LogContext])
        ) thenReturn EitherT.leftT(AccountNotFound())
        Get(s"/$provider/token?userId=asd") ~> addCredentials(
          BasicHttpCredentials("testuser", "testpass")
        ) ~> route ~> check {
          status shouldEqual StatusCodes.NotFound
          responseAs[ErrorResponse] shouldBe ErrorResponse("Account not found")
        }
        verify(services.getOauthServiceByName(provider), times(1))
          .getAccessTokenForUser(eqTo("asd"))(any[MonadError[Future, Throwable]], any[LogContext])
      }
      "reject if query param is missing" in new BaseTestScope {
        Get(s"/$provider/token") ~> addCredentials(BasicHttpCredentials("testuser", "testpass")) ~> route ~> check {
          rejection shouldBe a[MissingQueryParamRejection]
        }
      }
      "reject if authentication is missing or is invalid" in new BaseTestScope {
        Get(s"/$provider/token?userId=asd") ~> route ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
      }
    }
  }
}
