package com.wanari.tutelar

import cats.Id
import com.wanari.tutelar.github.GithubConfigService
import com.wanari.tutelar.jwt.JwtConfigService
import scala.concurrent.duration._

class ConfigServiceSpec extends TestBase {
  "#getVersion" in {
    val service = new ConfigServiceImpl[Id]()
    service.getVersion shouldEqual "TestVersion"
  }
  "#getHostname" in {
    val service = new ConfigServiceImpl[Id]()
    service.getHostname shouldEqual "TestHostname"
  }
  "#getGithubConfig" in {
    val service = new ConfigServiceImpl[Id]()
    service.getGithubConfig shouldBe a[GithubConfigService[?[_]]]
  }
  "#getJwtConfig" in {
    val service = new ConfigServiceImpl[Id]()
    val config  = service.getJwtConfig
    config shouldBe a[JwtConfigService[?[_]]]
    config.getConfig.expirationTime.toSeconds shouldEqual 1.day.toSeconds
    config.getConfig.algorithm shouldEqual "HS256"
    config.getConfig.secret shouldEqual "secret"
    config.getConfig.privateKey shouldEqual "private"
    config.getConfig.publicKey shouldEqual "public"
  }
}
