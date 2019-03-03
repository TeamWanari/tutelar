package com.wanari.tutelar.providers.ldap
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi
import com.wanari.tutelar.core.ProviderApi._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class LdapApi(implicit val service: LdapService[Future], val callbackConfig: () => Future[CallbackConfig])
    extends ProviderApi {

  override def route(): Route = {
    pathPrefix("ldap") {
      path("login") {
        post {
          entity(as[LoginData]) { data =>
            completeLoginFlowWithJson(service.login(data.username, data.password))
          }
        } ~
          parameters(('username.as[String], 'password.as[String])) {
            case (username, password) =>
              completeLoginFlowWithRedirect(service.login(username, password))
          }
      }
    }
  }
}
