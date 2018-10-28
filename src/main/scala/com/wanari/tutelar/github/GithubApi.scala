package com.wanari.tutelar.github

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api

import scala.concurrent.Future
import scala.util.{Failure, Success}

class GithubApi(implicit service: GithubService[Future]) extends Api {

  def route(): Route = {
    pathPrefix("github") {
      path("login") {
        onComplete(service.generateIdentifierUrl) {
          case Success(value) => redirect(value, StatusCodes.Found)
          case _              => complete(StatusCodes.Unauthorized)
        }
      } ~
        path("callback") {
          parameterMap { params =>
            onComplete(service.authenticateWithCallback(params)) {
              case Success(value) => complete("")
              case Failure(_)     => complete(StatusCodes.Unauthorized)
            }

          }
        }
    }
  }

}
