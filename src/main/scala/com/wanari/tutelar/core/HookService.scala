package com.wanari.tutelar.core

import spray.json.{JsObject, RootJsonFormat}

trait HookService[F[_]] {
  def register(id: String, authType: String, data: JsObject): F[JsObject]
  def login(id: String, authType: String, data: JsObject): F[JsObject]
  def modify(id: String, authType: String, data: JsObject): F[Unit]
  def link(id: String, authType: String, data: JsObject): F[JsObject]
  def unlink(id: String, authType: String): F[Unit]
  def delete(id: String): F[Unit]
}

object HookService {
  sealed trait AuthConfig
  case class BasicAuthConfig(username: String, password: String) extends AuthConfig
  case class HookConfig(baseUrl: String, authConfig: AuthConfig)

  case class HookUserData(id: String, authType: String, data: Option[JsObject])
  case class HookDeleteData(id: String)

  import spray.json.DefaultJsonProtocol._
  implicit val hookRequestDataFormat: RootJsonFormat[HookUserData]  = jsonFormat3(HookUserData)
  implicit val hookDeleteDataFormat: RootJsonFormat[HookDeleteData] = jsonFormat1(HookDeleteData)
}
