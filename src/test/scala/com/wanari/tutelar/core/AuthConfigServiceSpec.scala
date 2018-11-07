package com.wanari.tutelar.core

import cats.Id
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase

class AuthConfigServiceSpec extends TestBase {

  val configRoot = ConfigFactory.load().getConfig("auth")

  "#getCallbackUrl" in {
    val service = new AuthConfigServiceImpl[Id](configRoot)
    service.getCallbackUrl shouldEqual "url?t=<<TOKEN>>"
  }
}
