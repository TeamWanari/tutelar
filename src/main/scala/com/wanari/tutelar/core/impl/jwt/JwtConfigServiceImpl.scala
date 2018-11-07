package com.wanari.tutelar.core.impl.jwt

import java.util.concurrent.TimeUnit

import cats.Monad
import com.typesafe.config.Config
import com.wanari.tutelar.core.impl.jwt.JwtConfigService.JwtConfig

import scala.concurrent.duration.FiniteDuration

class JwtConfigServiceImpl[F[_]: Monad](conf: => F[Config]) extends JwtConfigService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._

  lazy val getConfig: F[JwtConfig] = conf.flatMap { config =>
    JwtConfig(
      FiniteDuration(config.getDuration("expirationTime").getSeconds, TimeUnit.SECONDS),
      config.getString("algorithm"),
      config.getString("secret"),
      config.getString("privateKey"),
      config.getString("publicKey")
    ).pure
  }
}
