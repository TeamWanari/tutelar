package com.wanari.tutelar.core.config
import com.wanari.tutelar.TestBase

import scala.util.Try

class ServerConfigSpec extends TestBase {
  import cats.instances.try_._

  "#getVersion" in {
    val service = new ServerConfigImpl[Try]()
    service.getVersion.get shouldEqual "TestVersion"
  }
  "#getHostname" in {
    val service = new ServerConfigImpl[Try]()
    service.getHostname.get shouldEqual "TestHostname"
  }

  "can create RuntimeConf from conf" in {
    val service = new ServerConfigImpl[Try]()
    service.runtimeConfig.callbackConfig().get.failure shouldEqual "url?e=<<ERROR>>"
  }

}
