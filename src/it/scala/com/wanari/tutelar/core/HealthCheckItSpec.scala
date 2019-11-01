package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import cats.data.EitherT
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.core.Errors.{AppError, ErrorOr}
import com.wanari.tutelar.core.HealthCheckService._

import scala.concurrent.Future

class HealthCheckItSpec extends RouteTestBase {
  trait TestScope {
    def serviceResult: HealthCheckResult
    implicit lazy val service: HealthCheckService[Future] = new HealthCheckService[Future] {
      import cats.instances.future._
      override def getStatus: ErrorOr[Future, HealthCheckResult] = EitherT.pure[Future, AppError](serviceResult)
    }
    lazy val route = new HealthCheckApi().route
  }

  val basicResult = HealthCheckResult(
    success = true,
    "version",
    database = true,
    "time",
    1111L,
    Some("hash")
  )

  "GET /healthCheck" should {
    "return OK" in new TestScope {
      val serviceResult = basicResult
      Get("/healthCheck") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[HealthCheckResult] shouldEqual serviceResult
      }
    }
    "return 500 when not success" in new TestScope {
      val serviceResult = basicResult.copy(success = false)
      Get("/healthCheck") ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[HealthCheckResult] shouldEqual serviceResult
      }
    }
  }
}
