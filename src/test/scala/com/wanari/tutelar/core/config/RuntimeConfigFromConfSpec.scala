package com.wanari.tutelar.core.config
import cats.Id
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import concurrent.duration._

class RuntimeConfigFromConfSpec extends TestBase {
  val confFile = "runtime.conf"

  "reads the input file" in {
    val origin = new RuntimeConfigFromConf[Id](confFile)
    val dummy  = new RuntimeConfigFromConf[Id]("dummyconf.conf")
    origin.callbackConfig().accessDenied shouldEqual "accessDeniedUrl"
    dummy.callbackConfig().accessDenied shouldEqual "denied"
  }

  "oauth2 related" should {
    "#getFacebookConfig" in {
      val service = new RuntimeConfigFromConf[Id](confFile)
      service.facebookConfig() shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("public_profile")
      )
    }
    "#getGithubConfig" in {
      val service = new RuntimeConfigFromConf[Id](confFile)
      service.githubConfig() shouldBe OAuth2Config("https://lvh.me:9443", "clientId", "clientSecret", Seq("read:user"))
    }
    "#getGoogleConfig" in {
      val service = new RuntimeConfigFromConf[Id](confFile)
      service.googleConfig() shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("openid", "email", "profile")
      )
    }
  }
  "#getJwtConfig" in {
    val service = new RuntimeConfigFromConf[Id](confFile)
    val config  = service.jwtConfig()
    config shouldBe JwtConfig(
      1.day,
      "HS256",
      "secret",
      "private",
      "public"
    )
  }
  "#getCallbackConfig" in {
    val service = new RuntimeConfigFromConf[Id](confFile)
    val config  = service.callbackConfig()
    config.success shouldEqual "url?t=<<TOKEN>>"
    config.accessDenied shouldEqual "accessDeniedUrl"
  }
  "#getHookConfig" in {
    val service = new RuntimeConfigFromConf[Id](confFile)
    val config  = service.hookConfig()
    config shouldEqual HookConfig(
      "https://backend/hook",
      BasicAuthConfig("user", "pass")
    )
  }
  "#getLdapConfig" in {
    val service = new RuntimeConfigFromConf[Id](confFile)
    val config  = service.ldapConfig()
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

}
