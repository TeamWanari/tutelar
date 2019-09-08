package com.wanari.tutelar.core

import com.wanari.tutelar.core.Errors.ErrorOr
import spray.json.JsObject

trait CsrfService[F[_]] {
  def getCsrfToken(auther: String, data: JsObject): F[String]
  def checkCsrfToken(auther: String, str: String): ErrorOr[F, Unit]
}
