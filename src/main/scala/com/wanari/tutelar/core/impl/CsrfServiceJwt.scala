package com.wanari.tutelar.core.impl

import java.time.Clock

import cats.MonadError
import cats.data.EitherT
import com.wanari.tutelar.core.CsrfService
import com.wanari.tutelar.core.Errors.{AppError, ErrorOr, InvalidCsrfToken}
import com.wanari.tutelar.core.impl.CsrfServiceJwt.CsrfJwtConfig
import com.wanari.tutelar.util.DateTimeUtil
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration.Duration

class CsrfServiceJwt[F[_]: MonadError[*[_], Throwable]: DateTimeUtil](implicit csrfJwtConfig: () => F[CsrfJwtConfig])
    extends CsrfService[F] {
  import cats.syntax.functor._
  import com.wanari.tutelar.util.SpraySyntax._

  protected implicit val clock = Clock.systemDefaultZone()

  def getCsrfToken(auther: String, data: JsObject = JsObject.empty): F[String] = {
    csrfJwtConfig().map { config =>
      val claim =
        JwtClaim((data + ("auther" -> JsString(auther))).compactPrint).expiresIn(config.expirationTime.toSeconds)
      JwtSprayJson.encode(claim, config.key, JwtAlgorithm.HS256)
    }
  }

  def checkCsrfToken(auther: String, token: String): ErrorOr[F, Unit] = {
    for {
      config <- EitherT.right[AppError](csrfJwtConfig())
      data <- EitherT.fromOption(
        JwtSprayJson.decodeJson(token, config.key, Seq(JwtAlgorithm.HS256)).toOption,
        InvalidCsrfToken()
      )
      _ <- EitherT
        .rightT[F, AppError](!data.fields.get("auther").contains(JsString(auther)))
        .ensure(InvalidCsrfToken())(identity)
    } yield ()
  }

}

object CsrfServiceJwt {
  case class CsrfJwtConfig(key: String, expirationTime: Duration)
}
