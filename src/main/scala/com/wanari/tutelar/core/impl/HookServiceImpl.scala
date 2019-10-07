package com.wanari.tutelar.core.impl

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import cats.MonadError
import com.wanari.tutelar.core.HookService._
import com.wanari.tutelar.core.{EscherService, HookService}
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.util.LoggerUtil.{LogContext, Logger}

class HookServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit config: HookConfig,
    http: HttpWrapper[F],
    escher: EscherService[F]
) extends HookService[F] {
  import cats.syntax.applicative._
  import cats.syntax.applicativeError._
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import spray.json.DefaultJsonProtocol._
  import spray.json._

  private val logger = new Logger("HookService")

  override def register(id: String, externalId: String, authType: String, data: JsObject)(
      implicit ctx: LogContext
  ): F[JsObject] = {
    val dto = HookUserData(id, externalId, authType, Option(data)).toJson
    sendHookAndParse("/register", dto)
  }

  override def login(id: String, externalId: String, authType: String, data: JsObject)(
      implicit ctx: LogContext
  ): F[JsObject] = {
    val dto = HookUserData(id, externalId, authType, Option(data)).toJson
    sendHookAndParse("/login", dto)
  }

  override def modify(id: String, externalId: String, authType: String, data: JsObject)(
      implicit ctx: LogContext
  ): F[Unit] = {
    val dto = HookUserData(id, externalId, authType, Option(data)).toJson
    sendHookWithoutResponse("/modify", dto)
  }

  override def link(id: String, externalId: String, authType: String, data: JsObject)(
      implicit ctx: LogContext
  ): F[JsObject] = {
    val dto = HookUserData(id, externalId, authType, Option(data)).toJson
    sendHookAndParse("/link", dto)
  }

  override def unlink(id: String, externalId: String, authType: String)(implicit ctx: LogContext): F[Unit] = {
    val dto = HookUserData(id, externalId, authType, None).toJson
    sendHookWithoutResponse("/unlink", dto)
  }

  override def delete(id: String)(implicit ctx: LogContext): F[Unit] = {
    val dto = HookDeleteData(id).toJson
    sendHookWithoutResponse("/delete", dto)
  }

  private def sendHookWithoutResponse(endpoint: String, dto: JsValue)(implicit ctx: LogContext): F[Unit] = {
    sendHook(endpoint, dto)
      .map(_ => {})
      .recover {
        case HookDisabled() =>
      }
  }

  private def sendHookAndParse(endpoint: String, dto: JsValue)(implicit ctx: LogContext): F[JsObject] = {
    sendHook(endpoint, dto)
      .flatMap(http.unmarshalEntityTo[JsObject])
      .recover {
        case HookDisabled() => JsObject()
      }
  }

  private def sendHook(endpoint: String, data: JsValue)(implicit ctx: LogContext): F[HttpResponse] = {
    import com.wanari.tutelar.util.ApplicativeErrorSyntax._
    val baseUrl = config.baseUrl
    if (baseUrl.isEmpty) {
      HookDisabled().raise[F, HttpResponse]
    } else {
      val url     = baseUrl + endpoint
      val entity  = HttpEntity(ContentTypes.`application/json`, data.compactPrint)
      val request = HttpRequest(POST, url, entity = entity)
      authenticator(config.authConfig, request)
    }.flatMap(http.singleRequest)
      .recoverWith {
        case ex: HookDisabled => ex.raiseError
        case ex =>
          logger.warn(s"FAILED Send hook to $endpoint", ex)
          ex.raiseError
      }
  }

  private def authenticator(authConfig: AuthConfig, request: HttpRequest): F[HttpRequest] = {
    authConfig match {
      case BasicAuthConfig(username, password) =>
        request.addCredentials(BasicHttpCredentials(username, password)).pure[F]
      case EscherAuthConfig =>
        escher.signRequest("hook", request)
    }
  }

  private case class HookDisabled() extends Exception
}
