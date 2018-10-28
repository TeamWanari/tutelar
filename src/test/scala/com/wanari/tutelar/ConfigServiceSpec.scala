package com.wanari.tutelar

import cats.Id
import com.wanari.tutelar.github.GithubConfigService

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
}
