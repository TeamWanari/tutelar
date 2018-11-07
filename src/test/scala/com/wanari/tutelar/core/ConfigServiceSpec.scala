package com.wanari.tutelar.core

import cats.Id
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.impl.jwt.JwtConfigService
import com.wanari.tutelar.providers.ldap.LdapConfigService
import com.wanari.tutelar.providers.oauth2.OAuth2ConfigService

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
      service.getFacebookConfig shouldBe a[OAuth2ConfigService[?[_]]]
    }
    "#getGithubConfig" in {
      val service = new ConfigServiceImpl[Id]()
      service.getGithubConfig shouldBe a[OAuth2ConfigService[?[_]]]
    }
    "#getGoogleConfig" in {
      val service = new ConfigServiceImpl[Id]()
      service.getGoogleConfig shouldBe a[OAuth2ConfigService[?[_]]]
    }
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
  "#getAuthConfig" in {
    val service = new ConfigServiceImpl[Id]()
    val config  = service.getAuthConfig
    config shouldBe a[AuthConfigServiceImpl[?[_]]]
    config.getCallbackUrl shouldEqual "url?t=<<TOKEN>>"
  }
  "#getLdapConfig" in {
    val service = new ConfigServiceImpl[Id]()
    val config  = service.getLdapConfig
    config shouldBe a[LdapConfigService[?[_]]]
    config.getLdapUrl shouldEqual "ldap://1.2.3.4:389"
    config.getReadonlyUserPassword shouldEqual "readonlypw"
    config.getReadonlyUserWithNameSpace shouldEqual "cn=readonly,dc=example,dc=com"
    config.getUserSearchAttribute shouldEqual "cn"
    config.getUserSearchBaseDomain shouldEqual "ou=peaple,dc=example,dc=com"
    config.getUserSearchReturnAttributes shouldEqual Seq("cn", "sn", "email")
  }
}
