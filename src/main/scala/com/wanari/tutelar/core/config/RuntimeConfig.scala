package com.wanari.tutelar.core.config
import cats.Monad
import com.typesafe.config.Config
import com.wanari.tutelar.core.HookService.HookConfig
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig

trait RuntimeConfig[F[_]] {
  implicit val callbackConfig: () => F[CallbackConfig]
  implicit val jwtConfig: () => F[JwtConfig]
  implicit val hookConfig: () => F[HookConfig]
  implicit val emailServiceConfig: () => F[EmailProviderConfig]

  val facebookConfig: () => F[OAuth2Config]
  val githubConfig: () => F[OAuth2Config]
  val googleConfig: () => F[OAuth2Config]
  implicit val ldapConfig: () => F[LdapConfig]
}

object RuntimeConfig {
  def apply[F[_]: Monad](confTypeString: String, configConf: Config): RuntimeConfig[F] = {
    confTypeString match {
      case "conf" => new RuntimeConfigFromConf[F](configConf.getString("file"))
    }
  }
}
