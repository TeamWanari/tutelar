package com.wanari.tutelar.util

import cats.{Applicative, ApplicativeError}

object ApplicativeErrorSyntax {
  implicit class ErrorOps(private val t: Throwable) extends AnyVal {
    def raise[F[_], A](implicit F: ApplicativeError[F, Throwable]) = F.raiseError[A](t)
  }
  implicit class OptionErrorOps[A](private val option: Option[A]) extends AnyVal {
    def pureOrRaise[F[_]: ApplicativeError[?[_], Throwable]](t: => Throwable) =
      option.fold(t.raise[F, A])(Applicative[F].pure(_))
  }
}
