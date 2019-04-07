package com.wanari.tutelar.core

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api

import scala.concurrent.Future

class CoreApi(implicit val authService: AuthService[Future]) extends Api with AuthDirectives {
  override def route(): Route = {
    pathPrefix("core") {
      path("delete") {
        post {
          userAuth { userId =>
            onSuccess(authService.deleteUser(userId)) {
              complete(StatusCodes.OK)
            }
          }
        }
      }
    }
  }
}
