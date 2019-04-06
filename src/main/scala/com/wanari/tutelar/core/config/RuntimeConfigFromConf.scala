package com.wanari.tutelar.core.config
import java.util.concurrent.TimeUnit

import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.impl.database.DatabaseServiceProxy.DatabaseServiceProxyConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.OTP.OTPAlgorithm
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

import scala.concurrent.duration.FiniteDuration

class RuntimeConfigFromConf[F[_]: MonadError[?[_], Throwable]](filepath: String) extends RuntimeConfig[F] {
  import cats.syntax.applicative._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  private lazy val conf: Config = ConfigFactory.load(filepath)

  lazy val getRootUrl: F[String] = conf.getString("rootUrl").pure

  implicit val jwtConfig: () => F[JwtConfig]                            = readJwtConfig _
  implicit val callbackConfig: () => F[CallbackConfig]                  = readCallbackConfig _
  implicit val hookConfig: () => F[HookConfig]                          = readHookConfig _
  implicit val emailServiceConfig: () => F[EmailProviderConfig]         = readEmailServiceConfig _
  implicit val passwordSettings: () => F[PasswordSettings]              = readPasswordSettings _
  implicit val databaseProxyConfig: () => F[DatabaseServiceProxyConfig] = readDatabaseProxyConfig _

  val facebookConfig: () => F[OAuth2Config]    = () => readOauth2Config("oauth2.facebook")
  val githubConfig: () => F[OAuth2Config]      = () => readOauth2Config("oauth2.github")
  val googleConfig: () => F[OAuth2Config]      = () => readOauth2Config("oauth2.google")
  implicit val ldapConfig: () => F[LdapConfig] = readLdapConfig _
  implicit val totpConfig: () => F[TotpConfig] = readTOTPConfig _

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

  private def readCallbackConfig = {
    val config = conf.getConfig("callback")
    CallbackConfig(
      config.getString("success"),
      config.getString("failure")
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
      config.getString("userSearchReturnAttributes").split(",").toSeq,
      config.getString("userSearchReturnArrayAttributes").split(",").toSeq
    )
  }.pure

  private def readHookConfig = {
    val config = conf.getConfig("hook")
    val authConfig = config.getString("authType") match {
      case "basic" =>
        BasicAuthConfig(
          config.getString("basicAuth.username"),
          config.getString("basicAuth.password")
        )
    }
    HookConfig(
      config.getString("baseUrl"),
      authConfig
    )
  }.pure

  private def readEmailServiceConfig = {
    val config = conf.getConfig("email")
    EmailProviderConfig(
      config.getString("serviceUrl"),
      config.getString("serviceUsername"),
      config.getString("servicePassword"),
      config.getString("registerUrl"),
      config.getString("resetPasswordUrl")
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
    val config = conf.getConfig("passwordDifficulty")
    PasswordSettings(
      config.getString("pattern")
    )
  }.pure

  private def readDatabaseProxyConfig = {
    val config = conf.getConfig("database")
    DatabaseServiceProxyConfig(
      config.getString("type")
    )
  }.pure
}
