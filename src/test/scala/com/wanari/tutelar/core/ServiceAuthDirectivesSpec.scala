package com.wanari.tutelar.core

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.emarsys.escher.akka.http.config.EscherConfig
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.ServiceAuthDirectives.{BasicAuthConfig, EscherAuthConfig, JwtAuthConfig}
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ServiceAuthDirectivesSpec extends TestBase with ScalatestRouteTest with ScalaFutures {
  trait BasicTestScope extends ServiceAuthDirectives {
    implicit val ec: ExecutionContext                       = implicitly
    val authConfig: ServiceAuthDirectives.ServiceAuthConfig = BasicAuthConfig("user", "pass")
    lazy val escherConfig: EscherConfig                     = null

    val route: Route = path("test") {
      authenticateService {
        complete("")
      }
    }
  }

  trait EscherTestScope extends ServiceAuthDirectives {
    implicit val ec: ExecutionContext                       = implicitly
    val authConfig: ServiceAuthDirectives.ServiceAuthConfig = EscherAuthConfig(List("test-service"))
    lazy val escherConfig: EscherConfig                     = new EscherConfig(ConfigFactory.load().getConfig("escher-test"))

    def signed(r: HttpRequest, intervalMillis: Int = 15): HttpRequest = {
      signRequest("test-service")(executor, materializer)(r)
        .futureValue(timeout(1.second), interval(intervalMillis.millis))
    }

    val route: Route = path("test") {
      authenticateService {
        complete("")
      }
    }
  }

  trait JwtTestScope extends ServiceAuthDirectives {
    implicit val ec: ExecutionContext = implicitly
    val authConfig: ServiceAuthDirectives.ServiceAuthConfig = JwtAuthConfig(
      JwtConfig(1.seconds, "HS256", "secret", "", "")
    )
    lazy val escherConfig: EscherConfig = null

    val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-IDcSemACt8x4iTMCda8Yhe3iZaWbvV5XKSTbuAn0M"

    val route: Route = path("test") {
      authenticateService {
        complete("")
      }
    }
  }

  "ServiceAuthDirectives" when {
    "basic auth" should {
      "reject if wrong credentials" in new BasicTestScope {
        Get("/test") ~> addCredentials(BasicHttpCredentials("wrong", "pass")) ~> route ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
        }
      }
      "ok" in new BasicTestScope {
        Get("/test") ~> addCredentials(BasicHttpCredentials("user", "pass")) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }
    "escher" should {
      "reject if wrong credentials" in new EscherTestScope {
        Get("/test") ~> addCredentials(BasicHttpCredentials("user", "pass")) ~> route ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
        }
      }
      "ok" in new EscherTestScope {
        signed(Get("http://localhost:9000/test")) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }
    "jwt" should {
      "reject if wrong credentials" in new JwtTestScope {
        Get("/test") ~> addCredentials(BasicHttpCredentials("user", "pass")) ~> route ~> check {
          rejection shouldBe an[AuthenticationFailedRejection]
        }
      }
      "ok" in new JwtTestScope {
        Get("/test") ~> addCredentials(OAuth2BearerToken(validToken)) ~> route ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }
  }
}
