package com.wanari.tutelar.core.config
import java.util.concurrent.TimeUnit

import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.TracerService.TracerServiceConfig
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig

import scala.concurrent.duration.FiniteDuration

trait ServerConfig[F[_]] extends Initable[F] {
  def getEnabledModules: Seq[String]

  val runtimeConfig: RuntimeConfig[F]

  implicit def getMongoConfig: MongoConfig

  implicit def getDatabaseConfig: DatabaseConfig

  implicit def getTracerServiceConfig: TracerServiceConfig

  implicit def getJwtConfig: JwtConfig

  implicit def getCallbackConfig: CallbackConfig

  implicit def getHookConfig: HookConfig
}

class ServerConfigImpl[F[_]: MonadError[?[_], Throwable]]() extends ServerConfig[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  private lazy val configType              = conf.getString("conf.configType")
  lazy val runtimeConfig: RuntimeConfig[F] = RuntimeConfig(configType, conf.getConfig(configType))

  lazy val getEnabledModules: Seq[String] = {
    conf
      .getString("modulesEnabled")
      .split(',')
      .map(_.trim.toLowerCase)
      .filterNot(_.isEmpty)
      .toSeq
  }

  override def init: F[Unit] = {
    import com.wanari.tutelar.util.ApplicativeErrorSyntax._
    if (conf.isEmpty) WrongConfig("Config is empty!").raise
    else ().pure
  }

  override implicit def getMongoConfig: MongoConfig = {
    val config = conf.getConfig("database.mongo")
    MongoConfig(
      config.getString("uri"),
      config.getString("collection")
    )
  }

  override implicit def getDatabaseConfig: DatabaseConfig = {
    val config = conf.getConfig("database")
    DatabaseConfig(
      config.getString("type")
    )
  }

  override implicit def getTracerServiceConfig: TracerServiceConfig = {
    val config = conf.getConfig("tracer")
    TracerServiceConfig(
      config.getString("client")
    )
  }

  override implicit def getJwtConfig: JwtConfig = {
    val config = conf.getConfig("jwt")
    JwtConfig(
      FiniteDuration(config.getDuration("expirationTime").getSeconds, TimeUnit.SECONDS),
      config.getString("algorithm"),
      config.getString("secret"),
      config.getString("privateKey"),
      config.getString("publicKey")
    )
  }

  override implicit def getCallbackConfig: CallbackConfig = {
    val config = conf.getConfig("callback")
    CallbackConfig(
      config.getString("success"),
      config.getString("failure")
    )
  }

  override implicit def getHookConfig: HookConfig = {
    val config = conf.getConfig("hook")
    val authConfig = config.getString("authType") match {
      case "basic" =>
        BasicAuthConfig(
          config.getString("basicAuth.username"),
          config.getString("basicAuth.password")
        )
    }
    HookConfig(
      config.getString("baseUrl"),
      authConfig
    )
  }
}
