package com.wanari.tutelar.core.config
import cats.MonadError
import com.typesafe.config.Config
import com.wanari.tutelar.core.AmqpService.AmqpQueueConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

trait RuntimeConfig[F[_]] {
  implicit val emailServiceFactoryConfig: () => F[EmailServiceFactoryConfig]
  implicit val emailServiceHttpConfig: () => F[EmailServiceHttpConfig]
  implicit val totpConfig: () => F[TotpConfig]
  implicit def getAmqpQueueConfig(name: String): F[AmqpQueueConfig]

  val facebookConfig: () => F[OAuth2Config]
  val githubConfig: () => F[OAuth2Config]
  val googleConfig: () => F[OAuth2Config]
  implicit val ldapConfig: () => F[LdapConfig]
  implicit val passwordSettings: () => F[PasswordSettings]
}

object RuntimeConfig {
  def apply[F[_]: MonadError[?[_], Throwable]](confTypeString: String, configConf: Config): RuntimeConfig[F] = {
    confTypeString match {
      case "conf" => new RuntimeConfigFromConf[F](configConf.getString("file"))
    }
  }
}
