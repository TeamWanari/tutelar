package com.wanari.tutelar.core.config
import com.wanari.tutelar.TestBase

import scala.util.Try

class ServerConfigSpec extends TestBase {
  import cats.instances.try_._

  "#getHostname" in {
    val service = new ServerConfigImpl[Try]()
    service.getHostname.get shouldEqual "TestHostname"
  }

  "can create RuntimeConf from conf" in {
    val service = new ServerConfigImpl[Try]()
    service.runtimeConfig.callbackConfig().get.failure shouldEqual "url?e=<<ERROR>>"
  }

  "#isModuleEnabled" should {
    val service        = new ServerConfigImpl[Try]()
    val enabledModules = service.getEnabledModules
    "convert to lowecase" in {
      enabledModules.get should contain("testmodule1")
    }
    "trim config" in {
      enabledModules.get should contain("testmodule2")
    }
    "drop empty elements" in {
      enabledModules.get should not contain ("")
    }
  }

}
