package com.wanari.tutelar

import cats.Id

class ConfigServiceSpec extends TestBase {
  "#getVersion" in {
    val service = new ConfigServiceImpl[Id]()
    service.getVersion shouldEqual "TestVersion"
  }
  "#getHostname" in {
    val service = new ConfigServiceImpl[Id]()
    service.getHostname shouldEqual "TestHostname"
  }
}
