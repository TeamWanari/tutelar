package com.wanari.tutelar.core

import cats.Id
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config

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

  "oauth2 related" should {
    "#getFacebookConfig" in {
      val service = new ConfigServiceImpl[Id]()
      service.facebookConfig() shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("public_profile")
      )
    }
    "#getGithubConfig" in {
      val service = new ConfigServiceImpl[Id]()
      service.githubConfig() shouldBe OAuth2Config("https://lvh.me:9443", "clientId", "clientSecret", Seq("read:user"))
    }
    "#getGoogleConfig" in {
      val service = new ConfigServiceImpl[Id]()
      service.googleConfig() shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("openid", "email", "profile")
      )
    }
  }
  "#getJwtConfig" in {
    val service = new ConfigServiceImpl[Id]()
    val config  = service.jwtConfig()
    config shouldBe JwtConfig(
      1.day,
      "HS256",
      "secret",
      "private",
      "public"
    )
  }
  "#getAuthConfig" in {
    val service = new ConfigServiceImpl[Id]()
    val config  = service.authConfig()
    config.callback shouldEqual "url?t=<<TOKEN>>"
  }
  "#getLdapConfig" in {
    val service = new ConfigServiceImpl[Id]()
    val config  = service.ldapConfig()
    config shouldBe LdapConfig(
      "ldap://1.2.3.4:389",
      "cn=readonly,dc=example,dc=com",
      "readonlypw",
      "ou=peaple,dc=example,dc=com",
      "cn",
      Seq("cn", "sn", "email")
    )
  }
}
