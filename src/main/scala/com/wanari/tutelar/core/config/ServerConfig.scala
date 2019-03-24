package com.wanari.tutelar.core.config
import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}

trait ServerConfig[F[_]] {
  def getVersion: F[String]
  def getHostname: F[String]

  val runtimeConfig: RuntimeConfig[F]
}

class ServerConfigImpl[F[_]: MonadError[?[_], Throwable]]() extends ServerConfig[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  lazy val getVersion: F[String]  = conf.getString("version").pure
  lazy val getHostname: F[String] = conf.getString("hostname").pure

  private lazy val configType              = conf.getString("configType")
  lazy val runtimeConfig: RuntimeConfig[F] = RuntimeConfig(configType, conf.getConfig(configType))
}
