package com.wanari.tutelar.ldap
import cats.Monad
import com.typesafe.config.Config

trait LdapConfigService[F[_]] {
  def getLdapUrl: F[String]
  def getReadonlyUserWithNameSpace: F[String]
  def getReadonlyUserPassword: F[String]
  def getUserSearchBaseDomain: F[String]
  def getUserSearchAttribute: F[String]
  def getUserSearchReturnAttributes: F[Seq[String]]
}

class LdapConfigServiceImpl[F[_]: Monad](conf: => F[Config]) extends LdapConfigService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._

  lazy val getLdapUrl: F[String]                   = conf.flatMap(_.getString("url").pure)
  lazy val getReadonlyUserWithNameSpace: F[String] = conf.flatMap(_.getString("readonlyUserWithNamespace").pure)
  lazy val getReadonlyUserPassword: F[String]      = conf.flatMap(_.getString("readonlyUserPassword").pure)
  lazy val getUserSearchBaseDomain: F[String]      = conf.flatMap(_.getString("userSearchBaseDomain").pure)
  lazy val getUserSearchAttribute: F[String]       = conf.flatMap(_.getString("userSearchAttribute").pure)
  lazy val getUserSearchReturnAttributes: F[Seq[String]] =
    conf.flatMap(_.getString("userSearchReturnAttributes").split(",").toSeq.pure)
}
