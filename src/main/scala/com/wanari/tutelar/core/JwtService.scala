package com.wanari.tutelar.core

import com.wanari.tutelar.Initable
import spray.json.JsObject

trait JwtService[F[_]] extends Initable[F] {
  def encode(data: JsObject): F[String]
  def decode(token: String): F[JsObject]
  def validateAndDecode(token: String): F[JsObject]
  def validate(token: String): F[Boolean]
}
