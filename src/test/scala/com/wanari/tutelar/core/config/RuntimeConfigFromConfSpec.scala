package com.wanari.tutelar.core.config
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.AmqpService.AmqpQueueConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceRabbitMqImpl.EmailServiceRabbitMqConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

import scala.util.{Failure, Success, Try}

class RuntimeConfigFromConfSpec extends TestBase {
  import cats.instances.try_._
  val confFile = "runtime.conf"

  "reads the input file" in {
    val origin = new RuntimeConfigFromConf[Try](confFile)
    val dummy  = new RuntimeConfigFromConf[Try]("dummyconf.conf")
    origin.githubConfig().get.rootUrl shouldBe "https://lvh.me:9443"
    dummy.githubConfig().get.rootUrl shouldBe "ROOR_URL_DUMMY"
  }

  "oauth2 related" should {
    "#facebookConfig" in {
      val service = new RuntimeConfigFromConf[Try](confFile)
      service.facebookConfig().get shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("public_profile")
      )
    }
    "#githubConfig" in {
      val service = new RuntimeConfigFromConf[Try](confFile)
      service.githubConfig().get shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("read:user")
      )
    }
    "#googleConfig" in {
      val service = new RuntimeConfigFromConf[Try](confFile)
      service.googleConfig().get shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("openid", "email", "profile")
      )
    }
  }
  "#ldapConfig" in {
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
  "#totpConfig" in {
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
  "#passwordSettings" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.passwordSettings().get
    config shouldBe PasswordSettings(
      "PATTERN"
    )
  }
  "#emailServiceHttpConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.emailServiceHttpConfig().get
    config shouldBe EmailServiceHttpConfig(
      "URL",
      "USERNAME",
      "SECRET"
    )
  }
  "#emailServiceRabbitMqConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.emailServiceRabbitMqConfig().get
    config shouldBe EmailServiceRabbitMqConfig(
      "QUEUE"
    )
  }
  "#emailServiceFactoryConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    val config  = service.emailServiceFactoryConfig().get
    config shouldBe EmailServiceFactoryConfig(
      "TYPE"
    )
  }
  "#getAmqpQueueConfig" in {
    val service = new RuntimeConfigFromConf[Try](confFile)
    service.getAmqpQueueConfig("email_service") shouldBe Success(AmqpQueueConfig(Some("RK"), Some("EX"), 777))
    service.getAmqpQueueConfig("random") shouldBe a[Failure[_]]
  }
}
