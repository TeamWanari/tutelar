package com.wanari.tutelar.core.impl

import java.time.Clock

import cats.MonadError
import com.wanari.tutelar.core.CsrfService
import com.wanari.tutelar.core.Errors.InvalidCsrfToken
import com.wanari.tutelar.core.impl.CsrfServiceJwt.CsrfJwtConfig
import com.wanari.tutelar.util.DateTimeUtil
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration.Duration

class CsrfServiceJwt[F[_]: MonadError[?[_], Throwable]: DateTimeUtil](implicit csrfJwtConfig: () => F[CsrfJwtConfig])
    extends CsrfService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._
  import com.wanari.tutelar.util.SpraySyntax._

  def getCsrfToken(auther: String, data: JsObject = JsObject.empty): F[String] = {
    csrfJwtConfig().map { config =>
      val claim =
        JwtClaim((data + ("auther" -> JsString(auther))).compactPrint).expiresIn(config.expirationTime.toSeconds)
      JwtSprayJson.encode(claim, config.key, JwtAlgorithm.HS256)
    }
  }

  def checkCsrfToken(auther: String, token: String): F[Unit] = {
    csrfJwtConfig().map { config =>
      JwtSprayJson
        .decodeJson(token, config.key, Seq(JwtAlgorithm.HS256))
        .fold(_.raise[F, JsObject], _.pure)
        .map(_.fields.get("auther").contains(JsString(auther)))
        .flatMap(_.pureUnitOrRise(InvalidCsrfToken()))
    }
  }

}

object CsrfServiceJwt {
  case class CsrfJwtConfig(key: String, expirationTime: Duration)
}
