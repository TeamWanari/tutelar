package com.wanari.tutelar.core

import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.emarsys.escher.akka.http.config.EscherConfig
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.ServiceAuthDirectives.{BasicAuthConfig, EscherAuthConfig}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class ServiceAuthDirectivesSpec extends TestBase with ScalatestRouteTest with ScalaFutures {
  trait BasicTestScope extends ServiceAuthDirectives {
    override def getServiceAuthConfig: String => ServiceAuthDirectives.ServiceAuthConfig =
      _ => BasicAuthConfig("user", "pass")
    override lazy val escherConfig: EscherConfig = null

    val route = path("test") {
      authenticateService("") {
        complete("")
      }
    }
  }

  trait EscherTestScope extends ServiceAuthDirectives {
    override def getServiceAuthConfig: String => ServiceAuthDirectives.ServiceAuthConfig =
      _ => EscherAuthConfig(List("test-service"))
    override lazy val escherConfig: EscherConfig = new EscherConfig(ConfigFactory.load().getConfig("escher-test"))

    def signed(r: HttpRequest, intervalMillis: Int = 15): HttpRequest = {
      signRequest("test-service")(executor, materializer)(r)
        .futureValue(timeout(1.second), interval(intervalMillis.millis))
    }

    val route = path("test") {
      authenticateService("") {
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
  }
}
