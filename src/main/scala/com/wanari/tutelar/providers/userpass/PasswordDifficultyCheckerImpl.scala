package com.wanari.tutelar.providers.userpass

import cats.MonadError
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings

class PasswordDifficultyCheckerImpl[F[_]: MonadError[*[_], Throwable]](implicit config: () => F[PasswordSettings])
    extends PasswordDifficultyChecker[F] {
  import cats.syntax.functor._

  override def isValid(password: String): F[Boolean] = {
    config().map { settings =>
      settings.pattern.r.findFirstIn(password).isDefined
    }
  }
}

object PasswordDifficultyCheckerImpl {
  case class PasswordSettings(pattern: String)
}
