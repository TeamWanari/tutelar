package com.wanari.tutelar.core

import spray.json.{JsObject, RootJsonFormat}

trait HookService[F[_]] {
  def register(id: String, externalId: String, authType: String, data: JsObject): F[JsObject]
  def login(id: String, externalId: String, authType: String, data: JsObject): F[JsObject]
  def modify(id: String, externalId: String, authType: String, data: JsObject): F[Unit]
  def link(id: String, externalId: String, authType: String, data: JsObject): F[JsObject]
  def unlink(id: String, externalId: String, authType: String): F[Unit]
  def delete(id: String): F[Unit]
}

object HookService {
  sealed trait AuthConfig
  case class BasicAuthConfig(username: String, password: String) extends AuthConfig
  case class HookConfig(baseUrl: String, authConfig: AuthConfig)

  case class HookUserData(id: String, externalId: String, authType: String, data: Option[JsObject])
  case class HookDeleteData(id: String)

  import spray.json.DefaultJsonProtocol._
  implicit val hookRequestDataFormat: RootJsonFormat[HookUserData]  = jsonFormat4(HookUserData)
  implicit val hookDeleteDataFormat: RootJsonFormat[HookDeleteData] = jsonFormat1(HookDeleteData)
}
