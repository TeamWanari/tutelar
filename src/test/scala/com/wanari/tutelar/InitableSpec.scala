package com.wanari.tutelar

import com.emarsys.escher.akka.http.config.EscherConfig
import com.wanari.tutelar.core.HookService.HookConfig
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.TracerService.TracerServiceConfig
import com.wanari.tutelar.core.impl.JwtServiceImpl
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.database.PostgresDatabaseService
import com.wanari.tutelar.core.{AmqpService, ConfigService, ExpirationService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl
import com.wanari.tutelar.providers.userpass.email.{EmailServiceFactory, EmailServiceHttpImpl}
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl
import io.jaegertracing.Configuration
import org.slf4j.LoggerFactory

import scala.util.{Success, Try}

class InitableSpec extends TestBase {

  trait TestScope {
    var called = false
    var loaded = false
    lazy val service = new Initable[Try] {
      loaded = true
      override def init: Try[Unit] = {
        called = true
        Success(())
      }
    }
    implicit val logger = LoggerFactory.getLogger("test")
    implicit val config = new ConfigService {
      override def getEnabledModules: Seq[String]                                             = Seq("modulename")
      override def getMongoConfig: MongoConfig                                                = ???
      override def getDatabaseConfig: DatabaseConfig                                          = ???
      override def getTracerServiceConfig: TracerServiceConfig                                = ???
      override def getJwtConfigByName(name: String): JwtServiceImpl.JwtConfig                 = ???
      override def getCallbackConfig: CallbackConfig                                          = ???
      override def getHookConfig: HookConfig                                                  = ???
      override def getAmqpConfig: AmqpService.AmqpConfig                                      = ???
      override def emailServiceFactoryConfig: EmailServiceFactory.EmailServiceFactoryConfig   = ???
      override def emailServiceHttpConfig: EmailServiceHttpImpl.EmailServiceHttpConfig        = ???
      override def totpConfig: TotpServiceImpl.TotpConfig                                     = ???
      override def getAmqpQueueConfig(name: String): AmqpService.AmqpQueueConfig              = ???
      override def facebookConfig: OAuth2Service.OAuth2Config                                 = ???
      override def githubConfig: OAuth2Service.OAuth2Config                                   = ???
      override def googleConfig: OAuth2Service.OAuth2Config                                   = ???
      override def ldapConfig: LdapServiceImpl.LdapConfig                                     = ???
      override def passwordSettings: PasswordDifficultyCheckerImpl.PasswordSettings           = ???
      override def getPostgresConfig: PostgresDatabaseService.PostgresConfig                  = ???
      override def escherConfig: EscherConfig                                                 = ???
      override def jaegerConfig: Configuration                                                = ???
      override def providerExpirationConfigs: Map[String, ExpirationService.ExpirationConfig] = ???
    }
  }
  import cats.instances.try_._

  "Initiable" should {
    "#initializeIfEnabled" should {
      "call init if enabled" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename") shouldEqual Success(())
        called shouldBe true
      }
      "do not call init if disabled" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename2") shouldEqual Success(())
        called shouldBe false
      }
      "do not instantiating the service" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename2") shouldEqual Success(())
        loaded shouldBe false
      }
    }
  }

}
