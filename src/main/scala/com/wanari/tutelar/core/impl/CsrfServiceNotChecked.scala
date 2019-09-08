package com.wanari.tutelar.core.impl

import java.util.UUID

import cats.Applicative
import cats.data.EitherT
import com.wanari.tutelar.core.CsrfService
import com.wanari.tutelar.core.Errors.{AppError, ErrorOr}
import spray.json.JsObject

class CsrfServiceNotChecked[F[_]: Applicative] extends CsrfService[F] {
  import cats.syntax.applicative._

  def getCsrfToken(auther: String, data: JsObject = JsObject.empty): F[String] = UUID.randomUUID.toString.pure

  def checkCsrfToken(auther: String, str: String): ErrorOr[F, Unit] = EitherT.rightT[F, AppError]({})
}
