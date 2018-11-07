package com.wanari.tutelar.providers.oauth2

import cats.Id
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase

class OAuth2ConfigServiceSpec extends TestBase {

  val configRoot = ConfigFactory.load().getConfig("github")

  "#getRootUrl" in {
    val service = new OAuth2ConfigServiceImpl[Id]("rootUrl", configRoot)
    service.getRootUrl shouldBe "rootUrl"
  }

  "#getClientId" in {
    val service = new OAuth2ConfigServiceImpl[Id]("", configRoot)
    service.getClientId shouldBe "clientId"
  }
  "#getClientSecret" in {
    val service = new OAuth2ConfigServiceImpl[Id]("", configRoot)
    service.getClientSecret shouldBe "clientSecret"
  }
  "#getScopes" in {
    val service = new OAuth2ConfigServiceImpl[Id]("", configRoot)
    service.getScopes shouldBe Seq("read:user")
  }

}
