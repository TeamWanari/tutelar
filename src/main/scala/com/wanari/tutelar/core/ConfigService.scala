package com.wanari.tutelar.core

import java.util.concurrent.TimeUnit

import cats.Monad
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.core.AuthService.AuthConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config

import scala.concurrent.duration.FiniteDuration

class ConfigServiceImpl[F[_]: Monad]() extends ConfigService[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  lazy val getVersion: F[String]  = conf.getString("version").pure
  lazy val getHostname: F[String] = conf.getString("hostname").pure
  lazy val getRootUrl: F[String]  = conf.getString("rootUrl").pure

  implicit val jwtConfig: () => F[JwtConfig]   = readJwtConfig _
  implicit val authConfig: () => F[AuthConfig] = readAuthConfig _

  val facebookConfig: () => F[OAuth2Config]    = () => readOauth2Config("oauth2.facebook")
  val githubConfig: () => F[OAuth2Config]      = () => readOauth2Config("oauth2.github")
  val googleConfig: () => F[OAuth2Config]      = () => readOauth2Config("oauth2.google")
  implicit val ldapConfig: () => F[LdapConfig] = readLdapConfig _

  private def readJwtConfig = {
    val config = conf.getConfig("jwt")
    JwtConfig(
      FiniteDuration(config.getDuration("expirationTime").getSeconds, TimeUnit.SECONDS),
      config.getString("algorithm"),
      config.getString("secret"),
      config.getString("privateKey"),
      config.getString("publicKey")
    )
  }.pure

  private def readAuthConfig = {
    val config = conf.getConfig("auth")
    AuthConfig(
      config.getString("callbackUrl")
    )
  }.pure

  private def readLdapConfig = {
    val config = conf.getConfig("ldap")
    LdapConfig(
      config.getString("url"),
      config.getString("readonlyUserWithNamespace"),
      config.getString("readonlyUserPassword"),
      config.getString("userSearchBaseDomain"),
      config.getString("userSearchAttribute"),
      config.getString("userSearchReturnAttributes").split(",").toSeq
    )
  }.pure

  private def readOauth2Config(name: String) = {
    val config = conf.getConfig(name)
    OAuth2Config(
      conf.getString("rootUrl"),
      config.getString("clientId"),
      config.getString("clientSecret"),
      config.getString("scopes").split(",").toSeq
    )
  }.pure
}

trait ConfigService[F[_]] {
  def getVersion: F[String]
  def getHostname: F[String]

  implicit val authConfig: () => F[AuthConfig]
  implicit val jwtConfig: () => F[JwtConfig]

  val facebookConfig: () => F[OAuth2Config]
  val githubConfig: () => F[OAuth2Config]
  val googleConfig: () => F[OAuth2Config]
  implicit val ldapConfig: () => F[LdapConfig]
}
