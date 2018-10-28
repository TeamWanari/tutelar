package com.wanari.tutelar.github

import cats.Id
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase

class GithubConfigServiceSpec extends TestBase {

  val configRoot = ConfigFactory.load().getConfig("github")

  "#getClientId" in {
    val service = new GithubConfigServiceImpl[Id](configRoot)
    service.getClientId shouldBe "clientId"
  }
  "#getClientSecret" in {
    val service = new GithubConfigServiceImpl[Id](configRoot)
    service.getClientSecret shouldBe "clientSecret"
  }
  "#getScopes" in {
    val service = new GithubConfigServiceImpl[Id](configRoot)
    service.getScopes shouldBe Seq("read:user")
  }

}
