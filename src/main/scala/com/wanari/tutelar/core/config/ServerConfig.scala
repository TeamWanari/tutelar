package com.wanari.tutelar.core.config
import java.util.concurrent.TimeUnit

import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.AmqpService.AmqpConfig
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.TracerService.TracerServiceConfig
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig

import scala.concurrent.duration.FiniteDuration
import scala.io.Source
import scala.util.Try

trait ServerConfig[F[_]] extends Initable[F] {
  def getEnabledModules: Seq[String]

  val runtimeConfig: RuntimeConfig[F]

  implicit def getMongoConfig: MongoConfig

  implicit def getDatabaseConfig: DatabaseConfig

  implicit def getTracerServiceConfig: TracerServiceConfig

  implicit def getJwtConfigByName(name: String): JwtConfig

  implicit def getCallbackConfig: CallbackConfig

  implicit def getHookConfig: HookConfig

  implicit def getAmqpConfig: AmqpConfig
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
      readFromFileOrConf(config, "uri"),
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
          readFromFileOrConf(config, "basicAuth.password")
        )
    }
    HookConfig(
      config.getString("baseUrl"),
      authConfig
    )
  }

  override implicit def getAmqpConfig: AmqpConfig = {
    val config = conf.getConfig("amqp")
    AmqpConfig(
      readFromFileOrConf(config, "uri")
    )
  }

  private def readFromFileOrConf(config: Config, key: String): String = {
    Try(Source.fromFile(config.getString(s"${key}File")).mkString).getOrElse(config.getString(key))
  }
}
