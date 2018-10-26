package com.wanari.tutelar

import cats.Applicative
import com.typesafe.config.{Config, ConfigFactory}

class ConfigServiceImpl[F[_]: Applicative]() extends ConfigService[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  def getVersion: F[String]  = conf.getString("version").pure
  def getHostname: F[String] = conf.getString("hostname").pure
}

trait ConfigService[F[_]] {
  def getVersion: F[String]
  def getHostname: F[String]
}
