package com.wanari.tutelar

import cats.Monad
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.jwt.{JwtConfigService, JwtConfigServiceImpl}
import com.wanari.tutelar.ldap.{LdapConfigService, LdapConfigServiceImpl}
import com.wanari.tutelar.oauth2.{OAuth2ConfigService, OAuth2ConfigServiceImpl}

class ConfigServiceImpl[F[_]: Monad]() extends ConfigService[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  lazy val getVersion: F[String]                     = conf.getString("version").pure
  lazy val getHostname: F[String]                    = conf.getString("hostname").pure
  lazy val getRootUrl: F[String]                     = conf.getString("rootUrl").pure
  lazy val getFacebookConfig: OAuth2ConfigService[F] = getOauth2Conf("facebook")
  lazy val getGithubConfig: OAuth2ConfigService[F]   = getOauth2Conf("github")
  lazy val getGoogleConfig: OAuth2ConfigService[F]   = getOauth2Conf("google")
  lazy val getJwtConfig: JwtConfigService[F]         = new JwtConfigServiceImpl[F](conf.getConfig("jwt").pure)
  lazy val getAuthConfig: AuthConfigService[F]       = new AuthConfigServiceImpl[F](conf.getConfig("auth").pure)
  lazy val getLdapConfig: LdapConfigService[F]       = new LdapConfigServiceImpl[F](conf.getConfig("ldap").pure)

  private def getOauth2Conf(name: String) = {
    new OAuth2ConfigServiceImpl[F](getRootUrl, conf.getConfig(name).pure)
  }
}

trait ConfigService[F[_]] {
  def getVersion: F[String]
  def getHostname: F[String]
  def getGithubConfig: OAuth2ConfigService[F]
  def getJwtConfig: JwtConfigService[F]
  def getFacebookConfig: OAuth2ConfigService[F]
  def getGoogleConfig: OAuth2ConfigService[F]
  def getAuthConfig: AuthConfigService[F]
  def getLdapConfig: LdapConfigService[F]
}
