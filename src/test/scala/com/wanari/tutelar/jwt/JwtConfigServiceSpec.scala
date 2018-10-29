package com.wanari.tutelar.jwt

import cats.Id
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.jwt.JwtConfigService.JwtConfig
import scala.concurrent.duration._

class JwtConfigServiceSpec extends TestBase {

  val configRoot = ConfigFactory.load().getConfig("jwt")

  "#getConfig" in {
    val service = new JwtConfigServiceImpl[Id](configRoot)
    service.getConfig shouldBe JwtConfig(
      86400.seconds,
      "HS256",
      "secret",
      "private",
      "public"
    )
  }

}
