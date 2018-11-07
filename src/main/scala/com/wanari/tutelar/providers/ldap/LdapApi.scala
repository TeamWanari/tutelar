package com.wanari.tutelar.providers.ldap
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api

import scala.concurrent.Future
import scala.util.Success

class LdapApi(implicit val service: LdapService[Future]) extends Api {

  override def route(): Route = {
    pathPrefix("ldap") {
      path("login") {
        parameters(('username.as[String], 'password.as[String])) {
          case (username, password) =>
            onComplete(service.login(username, password)) {
              case Success(value) => redirect(value, StatusCodes.Found)
              case _              => complete(StatusCodes.Unauthorized)
            }
        }
      }
    }
  }
}
