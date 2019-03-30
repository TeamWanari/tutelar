package com.wanari.tutelar.core.impl.jwt

import cats.MonadError
import com.wanari.tutelar.core.JwtService
import pdi.jwt.algorithms.{JwtAsymmetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}

import scala.concurrent.duration.Duration

object JwtServiceImpl {

  case class JwtConfig(
      expirationTime: Duration,
      algorithm: String,
      secret: String,
      privateKey: String,
      publicKey: String
  )

  def create[F[_]: MonadError[?[_], Throwable]](implicit jwtConfig: () => F[JwtConfig]): F[JwtService[F]] = {
    import cats.syntax.applicative._
    import cats.syntax.flatMap._
    import cats.syntax.functor._
    import com.wanari.tutelar.util.ApplicativeErrorSyntax._
    import spray.json._

    jwtConfig().flatMap { config =>
      JwtAlgorithm
        .optionFromString(config.algorithm)
        .collect {
          case algo: JwtHmacAlgorithm       => (algo, config.secret, config.secret)
          case algo: JwtAsymmetricAlgorithm => (algo, config.privateKey, config.publicKey)
        }
        .map {
          case (algo, encodeKey, decodeKey) =>
            new JwtService[F] {
              override def encode(data: JsObject): F[String] = {
                val claim = JwtClaim(data.compactPrint).expiresIn(config.expirationTime.toSeconds)
                JwtSprayJson.encode(claim, encodeKey, algo).pure
              }

              override def decode(token: String): F[JsObject] = {
                algo match {
                  case a: JwtHmacAlgorithm       => JwtSprayJson.decodeJson(token, decodeKey, Seq(a)).pureOrRise
                  case a: JwtAsymmetricAlgorithm => JwtSprayJson.decodeJson(token, decodeKey, Seq(a)).pureOrRise
                }
              }

              override def validate(token: String): F[Boolean] = {
                algo match {
                  case a: JwtHmacAlgorithm       => JwtSprayJson.isValid(token, decodeKey, Seq(a)).pure
                  case a: JwtAsymmetricAlgorithm => JwtSprayJson.isValid(token, decodeKey, Seq(a)).pure
                }
              }

              override def validateAndDecode(token: String): F[JsObject] = {
                for {
                  _    <- validate(token).map(_.pureUnitOrRise(new Exception("Invalid token")))
                  data <- decode(token)
                } yield data
              }
            }
        }
        .pureOrRaise(new Exception()) //TODO: Error handling

    }
  }
}
