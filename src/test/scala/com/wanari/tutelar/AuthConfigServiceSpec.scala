package com.wanari.tutelar

import cats.Id
import com.typesafe.config.ConfigFactory

class AuthConfigServiceSpec extends TestBase {

  val configRoot = ConfigFactory.load().getConfig("auth")

  "#getCallbackUrl" in {
    val service = new AuthConfigServiceImpl[Id](configRoot)
    service.getCallbackUrl shouldEqual "url?t=<<TOKEN>>"
  }
}
