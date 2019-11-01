package com.wanari.tutelar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest

class ApiSpec extends TestBase with ScalatestRouteTest {
  "#createRoute" when {
    "empty route" in {
      val result = Api.createRoute(Seq.empty)
      result shouldEqual Api.emptyRoute
    }

    "simple api" in {
      val route = Api.createRoute(Seq(createApi("api1")))
      Get("/api1") ~> route ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }

    "multiple api" in {
      val route = Api.createRoute(
        Seq(
          createApi("api1"),
          createApi("api2")
        )
      )
      Get("/api1") ~> route ~> check {
        responseAs[String] shouldEqual "ok"
      }
      Get("/api2") ~> route ~> check {
        responseAs[String] shouldEqual "ok"
      }
    }
  }

  private def createApi(name: String): Api = () => path(name)(complete("ok"))
}
