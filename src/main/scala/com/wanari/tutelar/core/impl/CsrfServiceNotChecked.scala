package com.wanari.tutelar.core.impl

import java.util.UUID

import cats.Applicative
import com.wanari.tutelar.core.CsrfService
import spray.json.JsObject

class CsrfServiceNotChecked[F[_]: Applicative] extends CsrfService[F] {
  import cats.syntax.applicative._

  def getCsrfToken(auther: String, data: JsObject = JsObject.empty): F[String] = UUID.randomUUID.toString.pure

  def checkCsrfToken(auther: String, str: String): F[Unit] = {}.pure
}
