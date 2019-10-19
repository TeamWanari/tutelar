package com.wanari.tutelar.core.impl

import java.util.UUID

import cats.Applicative
import cats.data.EitherT
import com.wanari.tutelar.core.CsrfService
import com.wanari.tutelar.core.Errors.{AppError, ErrorOr}

import scala.util.Try

class CsrfServiceNotChecked[F[_]: Applicative] extends CsrfService[F] {
  import cats.syntax.applicative._
  import com.wanari.tutelar.util.SpraySyntax._
  import spray.json._

  def getCsrfToken(auther: String, data: JsObject = JsObject.empty): F[String] = {
    (data + ("id" -> JsString(UUID.randomUUID.toString))).compactPrint.pure
  }

  def checkCsrfToken(auther: String, str: String): ErrorOr[F, JsObject] = {
    val data = Try(str.parseJson.asJsObject).getOrElse(JsObject.empty)
    EitherT.rightT[F, AppError](data)
  }
}
