package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api
import com.wanari.tutelar.core.HealthCheckService._

import scala.concurrent.Future
import scala.util.Success

class HealthCheckApi(implicit service: HealthCheckService[Future]) extends Api {
  def route: Route = {
    path("healthCheck") {
      get {
        withTrace("HealthCheck") { _ =>
          onComplete(service.getStatus.value) {
            case Success(Right(res)) if res.success => complete(res)
            case Success(Right(res))                => complete((StatusCodes.InternalServerError, res))
            case _                                  => complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }
}
