package com.wanari.tutelar.core

import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0}
import akka.http.scaladsl.server.Directives.{
  AsyncAuthenticator,
  Authenticator,
  authenticateBasic,
  authenticateOAuth2Async
}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.directives.HeaderDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import com.emarsys.escher.akka.http.EscherDirectives
import com.wanari.tutelar.core.ServiceAuthDirectives._
import com.wanari.tutelar.core.impl.JwtServiceImpl
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig

import scala.concurrent.{ExecutionContext, Future}

trait ServiceAuthDirectives extends EscherDirectives {
  implicit val ec: ExecutionContext
  protected def authConfig: ServiceAuthConfig
  protected lazy val authenticateService: Directive0 = {
    authConfig match {
      case BasicAuthConfig(username, password) =>
        val authenticator: Authenticator[Unit] = {
          case p @ Provided(u) if u == username && p.verify(password) => Some(())
          case _                                                      => None
        }
        authenticateBasic("", authenticator).map(_ => ())
      case CustomHeaderAuthConfig(headername, secret) =>
        val rejection = AuthorizationFailedRejection
        optionalHeaderValueByName(headername)
          .map(_.getOrElse(rejection))
          .require(secret.equals, rejection)
      case EscherAuthConfig(trustedServices) =>
        escherAuthenticate(trustedServices)
      case JwtAuthConfig(jwtConfig) =>
        import cats.instances.future._
        val service = new JwtServiceImpl[Future](jwtConfig)
        val authenticator: AsyncAuthenticator[Unit] = {
          case Provided(token) =>
            service
              .validate(token)
              .map { result => if (result) Some({}) else None }
              .recover { case x =>
                println(x)
                None
              }
          case _ => Future.successful(None)
        }
        authenticateOAuth2Async("", authenticator).map(_ => ())
      case _ => reject
    }
  }
}

object ServiceAuthDirectives {
  sealed trait ServiceAuthConfig
  case class BasicAuthConfig(username: String, password: String)        extends ServiceAuthConfig
  case class CustomHeaderAuthConfig(headername: String, secret: String) extends ServiceAuthConfig
  case class EscherAuthConfig(trustedServices: List[String])            extends ServiceAuthConfig
  case class JwtAuthConfig(jwtConfig: JwtConfig)                        extends ServiceAuthConfig
  case object AccessBlocked                                             extends ServiceAuthConfig
}
