package com.wanari.tutelar.core

import com.wanari.tutelar.Initable
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.{JsObject, RootJsonFormat}

trait HookService[F[_]] extends Initable[F] {
  def register(id: String, externalId: String, authType: String, data: JsObject)(implicit ctx: LogContext): F[JsObject]
  def login(id: String, externalId: String, authType: String, data: JsObject)(implicit ctx: LogContext): F[JsObject]
  def modify(id: String, externalId: String, authType: String, data: JsObject)(implicit ctx: LogContext): F[Unit]
  def link(id: String, externalId: String, authType: String, data: JsObject)(implicit ctx: LogContext): F[JsObject]
  def unlink(id: String, externalId: String, authType: String)(implicit ctx: LogContext): F[Unit]
  def delete(id: String)(implicit ctx: LogContext): F[Unit]
  def refreshToken(id: String, data: JsObject)(implicit ctx: LogContext): F[JsObject]
}

object HookService {
  sealed trait AuthConfig
  case class BasicAuthConfig(username: String, password: String)        extends AuthConfig
  case class CustomHeaderAuthConfig(headername: String, secret: String) extends AuthConfig
  case object EscherAuthConfig                                          extends AuthConfig
  case object JwtAuthConfig                                             extends AuthConfig
  case class HookConfig(baseUrl: String, enabled: Seq[String], authConfig: AuthConfig)

  case class HookUserData(id: String, externalId: String, authType: String, data: Option[JsObject])
  case class HookDeleteData(id: String)
  case class HookRefreshData(id: String, data: JsObject)

  import spray.json.DefaultJsonProtocol._
  implicit val hookRequestDataFormat: RootJsonFormat[HookUserData]    = jsonFormat4(HookUserData)
  implicit val hookDeleteDataFormat: RootJsonFormat[HookDeleteData]   = jsonFormat1(HookDeleteData)
  implicit val hookRefreshDataFormat: RootJsonFormat[HookRefreshData] = jsonFormat2(HookRefreshData)
}
