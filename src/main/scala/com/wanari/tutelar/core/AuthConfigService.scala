package com.wanari.tutelar.core

import cats.Monad
import com.typesafe.config.Config

trait AuthConfigService[F[_]] {
  def getCallbackUrl: F[String]
}

class AuthConfigServiceImpl[F[_]: Monad](conf: => F[Config]) extends AuthConfigService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._

  lazy val getCallbackUrl: F[String] = conf.flatMap(_.getString("callbackUrl").pure)
}
