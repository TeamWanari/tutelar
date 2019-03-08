package com.wanari.tutelar.providers.userpass.basic

import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.providers.userpass.UserPassApi

import scala.concurrent.Future

class BasicProviderApi(
    implicit val service: BasicProviderService[Future],
    val callbackConfig: () => Future[CallbackConfig]
) extends UserPassApi {
  override val servicePath: String = "basic"
}
