package com.wanari.tutelar.providers.ldap
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi
import com.wanari.tutelar.core.ProviderApi.CallbackConfig

import scala.concurrent.Future

class LdapApi(implicit val service: LdapService[Future], val callbackConfig: () => Future[CallbackConfig])
    extends ProviderApi {

  override def route(): Route = {
    pathPrefix("ldap") {
      path("login") {
        parameters(('username.as[String], 'password.as[String])) {
          case (username, password) =>
            completeLoginFlow(service.login(username, password))
        }
      }
    }
  }
}
