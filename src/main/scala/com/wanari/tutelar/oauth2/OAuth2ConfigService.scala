package com.wanari.tutelar.oauth2

import cats.Monad
import com.typesafe.config.Config

trait OAuth2ConfigService[F[_]] {
  def getRootUrl: F[String]
  def getClientId: F[String]
  def getClientSecret: F[String]
  def getScopes: F[Seq[String]]
}

class OAuth2ConfigServiceImpl[F[_]: Monad](rootUrl: => F[String], conf: => F[Config]) extends OAuth2ConfigService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._

  import collection.JavaConverters._

  lazy val getRootUrl: F[String]      = rootUrl
  lazy val getClientId: F[String]     = conf.flatMap(_.getString("clientId").pure)
  lazy val getClientSecret: F[String] = conf.flatMap(_.getString("clientSecret").pure)
  lazy val getScopes: F[Seq[String]]  = conf.flatMap(_.getStringList("scopes").asScala.toSeq.pure)
}
