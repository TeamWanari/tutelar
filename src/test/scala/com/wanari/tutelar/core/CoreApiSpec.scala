package com.wanari.tutelar.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.wanari.tutelar.TestBase
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.{reset, verify, when}
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
        when(authServiceMock.deleteUser(any[String])).thenReturn(Future.successful(()))
        Post("/core/delete") ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> coreApi.route() ~> check {
          status shouldEqual StatusCodes.OK
        }
        verify(authServiceMock).deleteUser("UserID")
      }
    }
  }

}
