package com.wanari.tutelar.core

import com.wanari.tutelar.core.AmqpService.{AmqpConfig, AmqpQueueConfig}
import com.wanari.tutelar.core.HookService.HookConfig
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.TracerService.TracerServiceConfig
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.database.PostgresDatabaseService.PostgresConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

trait ConfigService {
  def getEnabledModules: Seq[String]
  implicit def getMongoConfig: MongoConfig
  implicit def getPostgresConfig: PostgresConfig
  implicit def getDatabaseConfig: DatabaseConfig
  implicit def getTracerServiceConfig: TracerServiceConfig
  implicit def getJwtConfigByName(name: String): JwtConfig
  implicit def getCallbackConfig: CallbackConfig
  implicit def getHookConfig: HookConfig
  implicit def getAmqpConfig: AmqpConfig
  implicit def emailServiceFactoryConfig: EmailServiceFactoryConfig
  implicit def emailServiceHttpConfig: EmailServiceHttpConfig
  implicit def totpConfig: TotpConfig
  implicit def getAmqpQueueConfig(name: String): AmqpQueueConfig
  def facebookConfig: OAuth2Config
  def githubConfig: OAuth2Config
  def googleConfig: OAuth2Config
  implicit def ldapConfig: LdapConfig
  implicit def passwordSettings: PasswordSettings
}
