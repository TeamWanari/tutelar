package com.wanari.tutelar.core.config
import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig

trait ServerConfig[F[_]] extends Initable[F] {
  def getHostname: F[String]

  def getEnabledModules: F[Seq[String]]

  val runtimeConfig: RuntimeConfig[F]

  def getMongoConfig: F[MongoConfig]
}

class ServerConfigImpl[F[_]: MonadError[?[_], Throwable]]() extends ServerConfig[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

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

  override def getMongoConfig: F[MongoConfig] = {
    val config = conf.getConfig("mongo")
    MongoConfig(
      config.getString("uri"),
      config.getString("collection")
    ).pure
  }
}
