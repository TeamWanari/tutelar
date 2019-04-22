package com.wanari.tutelar.providers.userpass

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi
import com.wanari.tutelar.core.ProviderApi._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

trait UserPassApi extends ProviderApi {
  protected val service: UserPassService[Future]
  protected val servicePath: String

  override def route(): Route = {
    pathPrefix(servicePath) {
      path("login") {
        post {
          entity(as[LoginData]) { data =>
            withTrace(s"Login_$servicePath") { implicit ctx =>
              completeLoginFlowWithJson(service.login(data.username, data.password, data.data))
            }
          }
        }
      }
    }
  }
}
