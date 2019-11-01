package com.wanari.tutelar.providers.userpass

import cats.Monad
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings

class PasswordDifficultyCheckerImpl[F[_]: Monad](implicit config: PasswordSettings)
    extends PasswordDifficultyChecker[F] {
  import cats.syntax.applicative._

  override def isValid(password: String): F[Boolean] = {
    config.pattern.r.findFirstIn(password).isDefined.pure
  }
}

object PasswordDifficultyCheckerImpl {
  case class PasswordSettings(pattern: String)
}
