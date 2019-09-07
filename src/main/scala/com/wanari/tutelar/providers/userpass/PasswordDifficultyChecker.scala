package com.wanari.tutelar.providers.userpass

trait PasswordDifficultyChecker[F[_]] {
  def isValid(password: String): F[Boolean]
}
