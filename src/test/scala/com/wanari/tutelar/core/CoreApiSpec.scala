package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.EitherT
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.InvalidJwt
import com.wanari.tutelar.core.ProviderApi._
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

class CoreApiSpec extends TestBase with ScalatestRouteTest with BeforeAndAfterEach {

  val authServiceMock = mock[AuthService[Future]]

  override protected def beforeEach(): Unit = {
    reset(authServiceMock)
  }

  trait TestScope {
    val authResult: Option[String]       = Some("UserID")
    private implicit val authServiceImpl = authServiceMock
    val coreApi = new CoreApi {
      override protected def userAuthenticator: AsyncAuthenticator[String] = _ => Future.successful(authResult)
    }
  }

  "CoreApi" should {
    "/core/delete" should {
      "reject if auth failed" in new TestScope {
        override val authResult: Option[String] = None
        Post("/core/delete") ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> coreApi.route() ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
      }
      "call the auth service delete with the userid" in new TestScope {
        when(authServiceMock.deleteUser(any[String])(any[LogContext])).thenReturn(EitherT.rightT(()))
        Post("/core/delete") ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> coreApi.route() ~> check {
          status shouldEqual StatusCodes.OK
        }
        verify(authServiceMock).deleteUser(eqTo("UserID"))(any[LogContext])
      }
    }
    "/core/unlink" should {
      val authTypeEntity = HttpEntity(ContentTypes.`application/json`, """{"authType":"AuthType"}""")
      "reject if auth failed" in new TestScope {
        override val authResult: Option[String] = None
        Post("/core/unlink").withEntity(authTypeEntity) ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> coreApi
          .route() ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
      }
      "call the auth service unlink with the userid and authtype" in new TestScope {
        when(authServiceMock.unlink(any[String], any[String])(any[LogContext])).thenReturn(EitherT.rightT(()))
        Post("/core/unlink").withEntity(authTypeEntity) ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> coreApi
          .route() ~> check {
          status shouldEqual StatusCodes.OK
        }
        verify(authServiceMock).unlink(eqTo("UserID"), eqTo("AuthType"))(any[LogContext])
      }
    }
    "/core/refresh-token" should {
      val refreshTokenEntity = HttpEntity(ContentTypes.`application/json`, """{"refreshToken":"RefreshToken"}""")
      "return error when service cant create new tokens" in new TestScope {
        when(authServiceMock.refreshToken(any[String])(any[LogContext])).thenReturn(EitherT.leftT(InvalidJwt()))
        Post("/core/refresh-token").withEntity(refreshTokenEntity) ~> coreApi.route() ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
      }
      "call the auth service refresh-token with the token and return the new tokens" in new TestScope {
        when(authServiceMock.refreshToken(any[String])(any[LogContext]))
          .thenReturn(EitherT.rightT(TokenData("TOKEN", "REFRESH_TOKEN")))
        Post("/core/refresh-token").withEntity(refreshTokenEntity) ~> coreApi.route() ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[TokenData] shouldEqual TokenData("TOKEN", "REFRESH_TOKEN")
        }
        verify(authServiceMock).refreshToken(eqTo("RefreshToken"))(any[LogContext])
      }
    }
  }
}
