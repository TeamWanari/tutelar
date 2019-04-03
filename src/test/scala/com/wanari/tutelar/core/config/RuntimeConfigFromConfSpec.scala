package com.wanari.tutelar.core.config
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

import concurrent.duration._
import scala.util.Try

class RuntimeConfigFromConfSpec extends TestBase {
  import cats.instances.try_._
  val confFile = "runtime.conf"

  "reads the input file" in {
    val origin = new RuntimeConfigFromConf[Try](confFile)
    val dummy  = new RuntimeConfigFromConf[Try]("dummyconf.conf")
    origin.callbackConfig().get.failure shouldEqual "url?e=<<ERROR>>"
    dummy.callbackConfig().get.failure shouldEqual "denied"
  }

  "oauth2 related" should {
    "#getFacebookConfig" in {
      val service = new RuntimeConfigFromConf[Try](confFile)
      service.facebookConfig().get shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("public_profile")
      )
    }
    "#getGithubConfig" in {
      val service = new RuntimeConfigFromConf[Try](confFile)
      service.githubConfig().get shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("read:user")
      )
    }
    "#getGoogleConfig" in {
      val service = new RuntimeConfigFromConf[Try](confFile)
      service.googleConfig().get shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("openid", "email", "profile")
      )
    }
  }
  "#getJwtConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.jwtConfig().get
    config shouldBe JwtConfig(
      1.day,
      "HS256",
      "secret",
      "private",
      "public"
    )
  }
  "#getCallbackConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.callbackConfig().get
    config.success shouldEqual "url?t=<<TOKEN>>"
    config.failure shouldEqual "url?e=<<ERROR>>"
  }
  "#getHookConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.hookConfig().get
    config shouldEqual HookConfig(
      "https://backend/hook",
      BasicAuthConfig("user", "pass")
    )
  }
  "#getLdapConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.ldapConfig().get
    config shouldBe LdapConfig(
      "ldap://1.2.3.4:389",
      "cn=readonly,dc=example,dc=com",
      "readonlypw",
      "ou=peaple,dc=example,dc=com",
      "cn",
      Seq("cn", "sn", "email"),
      Seq("memberof")
    )
  }
  "#getTotpConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.totpConfig().get
    config shouldBe TotpConfig(
      "SHA1",
      1,
      30,
      6,
      false
    )
  }
  "#getPasswordSettings" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.passwordSettings().get
    config shouldBe PasswordSettings(
      "PATTERN"
    )
  }
}
