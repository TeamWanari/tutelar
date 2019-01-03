package com.wanari.tutelar.core.config
import cats.Id
import com.wanari.tutelar.TestBase

class ServerConfigSpec extends TestBase {
  "#getVersion" in {
    val service = new ServerConfigImpl[Id]()
    service.getVersion shouldEqual "TestVersion"
  }
  "#getHostname" in {
    val service = new ServerConfigImpl[Id]()
    service.getHostname shouldEqual "TestHostname"
  }

  "can create RuntimeConf from conf" in {
    val service = new ServerConfigImpl[Id]()
    service.runtimeConfig.callbackConfig().accessDenied shouldEqual "accessDeniedUrl"
  }

}
