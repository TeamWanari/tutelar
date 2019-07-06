package com.wanari.tutelar.core.config
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig

import scala.concurrent.duration._
import scala.util.Try

class ServerConfigSpec extends TestBase {
  import cats.instances.try_._

  "can create RuntimeConf from conf" in {
    val service = new ServerConfigImpl[Try]()
    service.runtimeConfig.githubConfig().get.rootUrl shouldEqual "https://lvh.me:9443"
  }

  "#isModuleEnabled" should {
    val service        = new ServerConfigImpl[Try]()
    val enabledModules = service.getEnabledModules
    "convert to lowecase" in {
      enabledModules should contain("testmodule1")
    }
    "trim config" in {
      enabledModules should contain("testmodule2")
    }
    "drop empty elements" in {
      enabledModules should not contain ("")
    }
  }

  "#getMongoConfig" in {
    val service = new ServerConfigImpl[Try]()
    service.getMongoConfig shouldEqual MongoConfig("URI", "COLLECTION")
  }

  "#getDatabaseConfig" in {
    val service = new ServerConfigImpl[Try]()
    service.getDatabaseConfig shouldBe DatabaseConfig(
      "DBTYPE"
    )
  }

  "#getShortTermJwtConfig" in {
    val service = new ServerConfigImpl[Try]()
    val config  = service.getJwtConfigByName("example")
    config shouldBe JwtConfig(
      1.day,
      "HS256",
      "secret",
      "private",
      "public"
    )
  }

  "#getCallbackConfig" in {
    val service = new ServerConfigImpl[Try]()
    val config  = service.getCallbackConfig
    config.success shouldEqual "url?t=<<TOKEN>>&rt=<<REFRESH_TOKEN>>"
    config.failure shouldEqual "url?e=<<ERROR>>"
  }

  "#getHookConfig" in {
    val service = new ServerConfigImpl[Try]()
    val config  = service.getHookConfig
    config shouldEqual HookConfig(
      "https://backend/hook",
      BasicAuthConfig("user", "pass")
    )
  }
}
