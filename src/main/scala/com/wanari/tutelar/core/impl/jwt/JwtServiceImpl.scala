package com.wanari.tutelar.core.impl.jwt

import cats.MonadError
import com.wanari.tutelar.core.JwtService
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import pdi.jwt.algorithms.{JwtAsymmetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._

import scala.concurrent.duration.Duration

class JwtServiceImpl[F[_]: MonadError[?[_], Throwable]](implicit jwtConfig: () => F[JwtConfig]) extends JwtService[F] {

  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  private def settings: F[Settings] = {
    jwtConfig().flatMap { config =>
      val expirationTime = config.expirationTime.toSeconds
      JwtAlgorithm
        .optionFromString(config.algorithm)
        .collect {
          case algo: JwtHmacAlgorithm       => Settings(algo, config.secret, config.secret, expirationTime)
          case algo: JwtAsymmetricAlgorithm => Settings(algo, config.privateKey, config.publicKey, expirationTime)
        }
        .pureOrRaise(new Exception("Wrong jwt config: not supported algorithm"))
    }
  }

  override def init: F[Unit] = {
    settings.map(_ => ())
  }

  override def encode(data: JsObject, expirationTime: Option[Duration] = None): F[String] = {
    settings.map { set =>
      val expTime = expirationTime.map(_.toSeconds).getOrElse(set.expirationTime)
      val claim   = JwtClaim(data.compactPrint).expiresIn(expTime)
      JwtSprayJson.encode(claim, set.encodeKey, set.algo)
    }
  }

  override def decode(token: String): F[JsObject] = {
    settings.flatMap { set =>
      set.algo match {
        case a: JwtHmacAlgorithm       => JwtSprayJson.decodeJson(token, set.decodeKey, Seq(a)).pureOrRise
        case a: JwtAsymmetricAlgorithm => JwtSprayJson.decodeJson(token, set.decodeKey, Seq(a)).pureOrRise
      }
    }
  }

  override def validateAndDecode(token: String): F[JsObject] = {
    for {
      _    <- validate(token).map(_.pureUnitOrRise(new Exception("Invalid token")))
      data <- decode(token)
    } yield data
  }

  override def validate(token: String): F[Boolean] = {
    settings.map { set =>
      set.algo match {
        case a: JwtHmacAlgorithm       => JwtSprayJson.isValid(token, set.decodeKey, Seq(a))
        case a: JwtAsymmetricAlgorithm => JwtSprayJson.isValid(token, set.decodeKey, Seq(a))
      }
    }
  }

  private case class Settings(algo: JwtAlgorithm, encodeKey: String, decodeKey: String, expirationTime: Long)
}

object JwtServiceImpl {
  case class JwtConfig(
      expirationTime: Duration,
      algorithm: String,
      secret: String,
      privateKey: String,
      publicKey: String
  )
}
