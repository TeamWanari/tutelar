package com.wanari.tutelar.core

import akka.http.scaladsl.server.Directives.authenticateOAuth2Async
import akka.http.scaladsl.server.directives.{AuthenticationDirective, Credentials}

import scala.concurrent.Future

trait AuthDirectives {
  val authService: AuthService[Future]

  type AsyncAuthenticator[T] = Credentials ⇒ Future[Option[T]]

  protected def userAuthenticator: AsyncAuthenticator[String] = {
    case Credentials.Provided(token) => authService.findUserIdInShortTermToken(token).value
    case _                           => Future.successful(None)
  }

  def userAuth: AuthenticationDirective[String] = authenticateOAuth2Async[String]("", userAuthenticator)

}
