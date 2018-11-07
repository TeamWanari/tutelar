package com.wanari.tutelar.core.impl

import java.util.UUID

import cats.Applicative
import com.wanari.tutelar.core.CsrfService

class CsrfServiceNotChecked[F[_]: Applicative] extends CsrfService[F] {
  import cats.syntax.applicative._

  def getCsrfToken(auther: String): F[String] = UUID.randomUUID.toString.pure

  def checkCsrfToken(auther: String, str: String): F[Unit] = {}.pure
}
