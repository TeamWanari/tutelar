package com.wanari.tutelar.core

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.{Authenticator, authenticateBasic, reject}
import akka.http.scaladsl.server.directives.Credentials.Provided
import com.emarsys.escher.akka.http.EscherDirectives
import com.wanari.tutelar.core.ServiceAuthDirectives.{BasicAuthConfig, EscherAuthConfig, ServiceAuthConfig}

trait ServiceAuthDirectives extends EscherDirectives {
  def getServiceAuthConfig: String => ServiceAuthConfig

  protected def authenticateService(authConfigPath: String): Directive0 = {
    val authConfig = getServiceAuthConfig(authConfigPath)
    authConfig match {
      case BasicAuthConfig(username, password) =>
        val authenticator: Authenticator[Unit] = {
          case p @ Provided(u) if u == username && p.verify(password) => Some(())
          case _                                                      => None
        }
        authenticateBasic("", authenticator).map(_ => ())
      case EscherAuthConfig(trustedServices) =>
        escherAuthenticate(trustedServices)
      case _ => reject
    }
  }
}

object ServiceAuthDirectives {
  sealed trait ServiceAuthConfig
  case class BasicAuthConfig(username: String, password: String) extends ServiceAuthConfig
  case class EscherAuthConfig(trustedServices: List[String])     extends ServiceAuthConfig
}
