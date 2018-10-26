package com.wanari.tutelar

import akka.http.scaladsl.model.StatusCodes

class HealthCheckItSpec extends RouteTestBase {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import HealthCheckService._

  "GET /healthCheck" should {
    "return OK" in new BaseTestScope {
      Get("/healthCheck") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "return with data" in new BaseTestScope {
      Get("/healthCheck") ~> route ~> check {
        responseAs[HealthCheckResult] shouldEqual HealthCheckResult(true, "ItTestVersion", "ItTestHostName", true)
      }
    }
  }
}
