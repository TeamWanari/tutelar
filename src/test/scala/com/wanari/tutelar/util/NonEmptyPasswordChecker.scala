package com.wanari.tutelar.util

import cats.ApplicativeError
import com.wanari.tutelar.providers.userpass.PasswordDifficultyChecker

class NonEmptyPasswordChecker[F[_]: ApplicativeError[*[_], Throwable]] extends PasswordDifficultyChecker[F] {
  import cats.syntax.applicative._

  override def isValid(password: String): F[Boolean] = (!password.isEmpty).pure

}
