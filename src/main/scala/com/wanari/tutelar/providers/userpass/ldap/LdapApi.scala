package com.wanari.tutelar.providers.userpass.ldap
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.providers.userpass.UserPassApi

import scala.concurrent.Future

class LdapApi(implicit val service: LdapService[Future], val callbackConfig: () => Future[CallbackConfig])
    extends UserPassApi {
  override val servicePath: String = "ldap"
}
