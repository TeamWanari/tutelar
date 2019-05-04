package com.wanari.tutelar.core.config

import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceRabbitMqImpl.EmailServiceRabbitMqConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.OTP.OTPAlgorithm
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

class RuntimeConfigFromConf[F[_]: MonadError[?[_], Throwable]](filepath: String) extends RuntimeConfig[F] {
  import cats.syntax.applicative._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  private lazy val conf: Config = ConfigFactory.load(filepath)

  implicit val emailServiceFactoryConfig: () => F[EmailServiceFactoryConfig]   = readEmailServiceFactoryConfig _
  implicit val emailServiceHttpConfig: () => F[EmailServiceHttpConfig]         = readEmailServiceConfig _
  implicit val emailServiceRabbitMqConfig: () => F[EmailServiceRabbitMqConfig] = readEmailServiceRabbitMqConfig _
  implicit val passwordSettings: () => F[PasswordSettings]                     = readPasswordSettings _

  val facebookConfig: () => F[OAuth2Config]    = () => readOauth2Config("oauth2.facebook")
  val githubConfig: () => F[OAuth2Config]      = () => readOauth2Config("oauth2.github")
  val googleConfig: () => F[OAuth2Config]      = () => readOauth2Config("oauth2.google")
  implicit val ldapConfig: () => F[LdapConfig] = readLdapConfig _
  implicit val totpConfig: () => F[TotpConfig] = readTOTPConfig _

  private def readLdapConfig = {
    val config = conf.getConfig("ldap")
    LdapConfig(
      config.getString("url"),
      config.getString("readonlyUserWithNamespace"),
      config.getString("readonlyUserPassword"),
      config.getString("userSearchBaseDomain"),
      config.getString("userSearchAttribute"),
      config.getString("userSearchReturnAttributes").split(",").toSeq,
      config.getString("userSearchReturnArrayAttributes").split(",").toSeq
    )
  }.pure

  private def readEmailServiceConfig = {
    val config = conf.getConfig("userpass.email.http")
    EmailServiceHttpConfig(
      config.getString("serviceUrl"),
      config.getString("serviceUsername"),
      config.getString("servicePassword")
    )
  }.pure

  private def readEmailServiceRabbitMqConfig = {
    val config = conf.getConfig("userpass.email.rabbit-mq")
    EmailServiceRabbitMqConfig(
      config.getString("queue")
    )
  }.pure

  private def readOauth2Config(name: String) = {
    val config = conf.getConfig(name)
    OAuth2Config(
      conf.getString("oauth2.rootUrl"),
      config.getString("clientId"),
      config.getString("clientSecret"),
      config.getString("scopes").split(",").toSeq
    )
  }.pure

  private def readTOTPConfig = {
    val config = conf.getConfig("totp")
    val algo   = config.getString("algorithm")
    OTPAlgorithm.algos
      .find(_.name == algo)
      .fold {
        (new Exception).raise[F, TotpConfig]
      } { _ =>
        TotpConfig(
          algo,
          config.getInt("window"),
          config.getDuration("period").toMillis / 1000 toInt,
          config.getInt("digits"),
          config.getBoolean("startFromCurrentTime")
        ).pure
      }
  }

  private def readPasswordSettings = {
    val config = conf.getConfig("userpass.passwordDifficulty")
    PasswordSettings(
      config.getString("pattern")
    )
  }.pure

  private def readEmailServiceFactoryConfig = {
    val config = conf.getConfig("userpass.email")
    EmailServiceFactoryConfig(
      config.getString("type")
    )
  }.pure
}
