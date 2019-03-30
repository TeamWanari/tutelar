package com.wanari.tutelar.core.config
import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.Initable

trait ServerConfig[F[_]] extends Initable[F] {
  def getVersion: F[String]
  def getHostname: F[String]

  def getEnabledModules: F[Seq[String]]

  val runtimeConfig: RuntimeConfig[F]
}

class ServerConfigImpl[F[_]: MonadError[?[_], Throwable]]() extends ServerConfig[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  lazy val getVersion: F[String]  = conf.getString("version").pure
  lazy val getHostname: F[String] = conf.getString("hostname").pure

  private lazy val configType              = conf.getString("configType")
  lazy val runtimeConfig: RuntimeConfig[F] = RuntimeConfig(configType, conf.getConfig(configType))

  lazy val getEnabledModules: F[Seq[String]] = {
    conf
      .getString("modulesEnabled")
      .split(',')
      .map(_.trim.toLowerCase)
      .filterNot(_.isEmpty)
      .toSeq
      .pure
  }

  override def init: F[Unit] = {
    import com.wanari.tutelar.util.ApplicativeErrorSyntax._
    if (conf.isEmpty) new Exception("Config is empty!").raise
    else ().pure
  }
}
