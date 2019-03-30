package com.wanari.tutelar.core

import com.wanari.tutelar.Initable
import spray.json.JsObject

import scala.concurrent.duration.Duration

trait JwtService[F[_]] extends Initable[F] {
  def encode(data: JsObject, expirationTime: Option[Duration] = None): F[String]
  def decode(token: String): F[JsObject]
  def validateAndDecode(token: String): F[JsObject]
  def validate(token: String): F[Boolean]
}
