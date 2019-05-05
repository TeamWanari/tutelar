package com.wanari.tutelar.providers.userpass

import cats.MonadError
import com.wanari.tutelar.core.Errors.WeakPassword
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings

class PasswordDifficultyCheckerImpl[F[_]: MonadError[?[_], Throwable]](implicit config: () => F[PasswordSettings])
    extends PasswordDifficultyChecker[F] {
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  override def validate(password: String): F[Unit] = {
    isValid(password).flatMap(_.pureUnitOrRise(WeakPassword()))
  }

  override def isValid(password: String): F[Boolean] = {
    config().map { settings =>
      settings.pattern.r.findFirstIn(password).isDefined
    }
  }
}

object PasswordDifficultyCheckerImpl {
  case class PasswordSettings(pattern: String)
}
