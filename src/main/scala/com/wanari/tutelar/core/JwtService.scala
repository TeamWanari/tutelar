package com.wanari.tutelar.core

import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.Errors.ErrorOr
import spray.json.JsObject

import scala.concurrent.duration.Duration

trait JwtService[F[_]] extends Initable[F] {
  def encode(data: JsObject, expirationTime: Option[Duration] = None): F[String]
  def validateAndDecode(token: String): ErrorOr[F, JsObject]
  def validate(token: String): F[Boolean]
}
