package com.wanari.tutelar.github

import cats.Monad
import com.typesafe.config.Config

trait GithubConfigService[F[_]] {
  def getClientId: F[String]
  def getClientSecret: F[String]
  def getScopes: F[Seq[String]]
}

class GithubConfigServiceImpl[F[_]: Monad](conf: => F[Config]) extends GithubConfigService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._

  import collection.JavaConverters._

  lazy val getClientId: F[String]     = conf.flatMap(_.getString("clientId").pure)
  lazy val getClientSecret: F[String] = conf.flatMap(_.getString("clientSecret").pure)
  lazy val getScopes: F[Seq[String]]  = conf.flatMap(_.getStringList("scopes").asScala.toSeq.pure)
}
