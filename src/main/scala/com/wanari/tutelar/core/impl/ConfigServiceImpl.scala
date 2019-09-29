package com.wanari.tutelar.core.impl

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.core.AmqpService.{AmqpConfig, AmqpQueueConfig}
import com.wanari.tutelar.core.ConfigService
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.TracerService.TracerServiceConfig
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.OTP.OTPAlgorithm
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

import scala.concurrent.duration.FiniteDuration
import scala.io.Source
import scala.util.Using

class ConfigServiceImpl() extends ConfigService {
  private lazy val conf: Config = ConfigFactory.load

  override lazy val getEnabledModules: Seq[String] = {
    conf
      .getString("modulesEnabled")
      .split(',')
      .map(_.trim.toLowerCase)
      .filterNot(_.isEmpty)
      .toSeq
  }

  override implicit lazy val getMongoConfig: MongoConfig = {
    val config = conf.getConfig("database.mongo")
    MongoConfig(
      readFromFileOrConf(config, "uri"),
      config.getString("collection")
    )
  }

  override implicit lazy val getDatabaseConfig: DatabaseConfig = {
    val config = conf.getConfig("database")
    DatabaseConfig(
      config.getString("type")
    )
  }

  override implicit lazy val getTracerServiceConfig: TracerServiceConfig = {
    val config = conf.getConfig("tracer")
    TracerServiceConfig(
      config.getString("client")
    )
  }

  override implicit def getJwtConfigByName(name: String): JwtConfig = {
    val config = conf.getConfig(s"jwt.$name")
    JwtConfig(
      FiniteDuration(config.getDuration("expirationTime").getSeconds, TimeUnit.SECONDS),
      config.getString("algorithm"),
      readFromFileOrConf(config, "secret"),
      readFromFileOrConf(config, "privateKey"),
      readFromFileOrConf(config, "publicKey")
    )
  }

  override implicit lazy val getCallbackConfig: CallbackConfig = {
    val config = conf.getConfig("callback")
    CallbackConfig(
      config.getString("success"),
      config.getString("failure")
    )
  }

  override implicit lazy val getHookConfig: HookConfig = {
    val config = conf.getConfig("hook")
    val authConfig = config.getString("authType") match {
      case "basic" =>
        BasicAuthConfig(
          config.getString("basicAuth.username"),
          readFromFileOrConf(config, "basicAuth.password")
        )
    }
    HookConfig(
      config.getString("baseUrl"),
      authConfig
    )
  }

  override implicit lazy val getAmqpConfig: AmqpConfig = {
    val config = conf.getConfig("amqp")
    AmqpConfig(
      readFromFileOrConf(config, "uri")
    )
  }

  override implicit lazy val emailServiceFactoryConfig: EmailServiceFactoryConfig = {
    val config = conf.getConfig("userpass.email")
    EmailServiceFactoryConfig(
      config.getString("type")
    )
  }

  override implicit lazy val emailServiceHttpConfig: EmailServiceHttpConfig = {
    val config = conf.getConfig("userpass.email.http")
    EmailServiceHttpConfig(
      config.getString("serviceUrl"),
      config.getString("serviceUsername"),
      readFromFileOrConf(config, "servicePassword")
    )
  }

  override implicit lazy val passwordSettings: PasswordSettings = {
    val config = conf.getConfig("userpass.passwordDifficulty")
    PasswordSettings(
      config.getString("pattern")
    )
  }

  override lazy val facebookConfig: OAuth2Config = readOauth2Config("oauth2.facebook")
  override lazy val githubConfig: OAuth2Config   = readOauth2Config("oauth2.github")
  override lazy val googleConfig: OAuth2Config   = readOauth2Config("oauth2.google")

  override implicit lazy val ldapConfig: LdapConfig = {
    val config = conf.getConfig("ldap")
    LdapConfig(
      config.getString("url"),
      config.getString("readonlyUserWithNamespace"),
      readFromFileOrConf(config, "readonlyUserPassword"),
      config.getString("userSearchBaseDomain"),
      config.getString("userSearchAttribute"),
      config.getString("userSearchReturnAttributes").split(",").toSeq,
      config.getString("userSearchReturnArrayAttributes").split(",").toSeq
    )
  }
  override implicit lazy val totpConfig: TotpConfig = {
    val config = conf.getConfig("totp")
    val algo   = config.getString("algorithm")
    OTPAlgorithm.algos
      .find(_.name == algo)
      .fold {
        throw WrongConfig(s"Unsupported TOTP algo: $algo")
      } { _ =>
        TotpConfig(
          algo,
          config.getInt("window"),
          config.getDuration("period").toMillis / 1000 toInt,
          config.getInt("digits"),
          config.getBoolean("startFromCurrentTime")
        )
      }
  }

  override implicit def getAmqpQueueConfig(name: String): AmqpQueueConfig = {
    val path: String = name match {
      case "email_service" => "userpass.email.amqp"
      case _               => throw new IllegalArgumentException(s"$name unknown AMQP type.")
    }
    AmqpQueueConfig(conf.getConfig(path))
  }

  private def readFromFileOrConf(config: Config, key: String): String = {
    lazy val fromConfig = config.getString(key)
    val fromFile        = Using(Source.fromFile(config.getString(s"${key}File")))(_.mkString)
    fromFile.getOrElse(fromConfig)
  }

  private def readOauth2Config(name: String): OAuth2Config = {
    val config = conf.getConfig(name)
    OAuth2Config(
      conf.getString("oauth2.rootUrl"),
      config.getString("clientId"),
      readFromFileOrConf(config, "clientSecret"),
      config.getString("scopes").split(",").toSeq
    )
  }

}
