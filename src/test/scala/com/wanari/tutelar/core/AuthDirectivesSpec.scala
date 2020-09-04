package com.wanari.tutelar.core

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.OptionT
import com.wanari.tutelar.TestBase
import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

class AuthDirectivesSpec extends TestBase with ScalatestRouteTest with BeforeAndAfterEach {

  val authServiceMock = mock[AuthService[Future]]

  override protected def beforeEach(): Unit = {
    reset(authServiceMock)
  }

  trait TestScope {
    val authDirectives = new AuthDirectives {
      override val authService: AuthService[Future] = authServiceMock
    }

    val route = authDirectives.userAuth { userId => complete(userId) }
  }

  "AuthDirectives" should {
    "#userAuth" should {
      "pass the bearer token to auth service" in new TestScope {
        when(authServiceMock.findUserIdInShortTermToken(any[String])).thenReturn(OptionT.some("ID"))
        Get() ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> route ~> check {
          responseAs[String] shouldEqual "ID"
        }
        verify(authServiceMock).findUserIdInShortTermToken("TOKEN")
      }
      "reject if auth service get token failed" in new TestScope {
        when(authServiceMock.findUserIdInShortTermToken(any[String])).thenReturn(OptionT.none[Future, String])
        Get() ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> route ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
      }
      "reject if missing auth header" in new TestScope {
        Get() ~> route ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
        verify(authServiceMock, never).findUserIdInShortTermToken(any[String])
      }
    }
  }
}
