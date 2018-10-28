package com.wanari.tutelar

import cats.Monad
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.github.{GithubConfigService, GithubConfigServiceImpl}

class ConfigServiceImpl[F[_]: Monad]() extends ConfigService[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  lazy val getVersion: F[String]                   = conf.getString("version").pure
  lazy val getHostname: F[String]                  = conf.getString("hostname").pure
  lazy val getGithubConfig: GithubConfigService[F] = new GithubConfigServiceImpl[F](conf.getConfig("github").pure)
}

trait ConfigService[F[_]] {
  def getVersion: F[String]
  def getHostname: F[String]
  def getGithubConfig: GithubConfigService[F]
}
