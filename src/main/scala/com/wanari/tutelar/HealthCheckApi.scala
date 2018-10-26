package com.wanari.tutelar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class HealthCheckApi(implicit service: HealthCheckService[Future]) extends Api {

  def route(): Route = {
    path("healthCheck") {
      get {
        onSuccess(service.getStatus) { result =>
          complete(result)
        }
      }
    }
  }
}
