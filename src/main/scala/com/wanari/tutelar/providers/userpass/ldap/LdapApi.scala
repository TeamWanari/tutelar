package com.wanari.tutelar.providers.userpass.ldap
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.emarsys.escher.akka.http.config.EscherConfig
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.ServiceAuthDirectives
import com.wanari.tutelar.core.ServiceAuthDirectives.ServiceAuthConfig
import com.wanari.tutelar.providers.userpass.UserPassApi
import com.wanari.tutelar.providers.userpass.ldap.LdapApi._
import com.wanari.tutelar.providers.userpass.ldap.LdapService.LdapUserListData
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future

class LdapApi(
    implicit val service: LdapService[Future],
    val callbackConfig: CallbackConfig,
    val getServiceAuthConfig: String => ServiceAuthConfig,
    val escherConfig: EscherConfig
) extends UserPassApi
    with ServiceAuthDirectives {
  override val servicePath: String = "ldap"

  override def route(): Route = {
    super.route() ~ pathPrefix(servicePath) {
      path("users") {
        authenticateService(authConfigPath = "ldap.ldapApi") {
          get {
            withTrace(s"List_users_$servicePath") { implicit ctx =>
              service.listUsers().toComplete
            }
          }
        }
      }
    }
  }
}

object LdapApi {
  implicit val formatLdapUSerListData: RootJsonFormat[LdapUserListData] = jsonFormat2(LdapUserListData)
}
