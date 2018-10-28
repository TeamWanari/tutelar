package com.wanari.tutelar

import java.util.UUID

import cats.Applicative

trait CsrfService[F[_]] {
  def getCsrfToken(auther: String): F[String]
  def checkCsrfToken(auther: String, str: String): F[Unit]
}

class CsrfServiceNotChecked[F[_]: Applicative] extends CsrfService[F] {
  import cats.syntax.applicative._

  def getCsrfToken(auther: String): F[String] = UUID.randomUUID.toString.pure

  def checkCsrfToken(auther: String, str: String): F[Unit] = {}.pure
}
