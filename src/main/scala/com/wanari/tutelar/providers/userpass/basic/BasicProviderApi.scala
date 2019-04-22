package com.wanari.tutelar.providers.userpass.basic

import akka.http.scaladsl.server.Directives.{as, entity, path, post}
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi.{CallbackConfig, LoginData}
import com.wanari.tutelar.providers.userpass.UserPassApi
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class BasicProviderApi(
    implicit val service: BasicProviderService[Future],
    val callbackConfig: () => Future[CallbackConfig]
) extends UserPassApi {
  override val servicePath: String = "basic"

  override def route(): Route = {
    super.route() ~
      pathPrefix(servicePath) {
        path("register") {
          post {
            entity(as[LoginData]) { data =>
              withTrace(s"Register_$servicePath") { implicit ctx =>
                completeLoginFlowWithJson(service.register(data.username, data.password, data.data))
              }
            }
          }
        }
      }
  }
}
