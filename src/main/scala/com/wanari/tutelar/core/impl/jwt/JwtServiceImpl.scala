package com.wanari.tutelar.core.impl.jwt

import java.security.Security
import java.time.Clock

import cats.MonadError
import cats.data.{EitherT, OptionT}
import com.wanari.tutelar.core.Errors.{AppError, ErrorOr, InvalidJwt, WrongConfig}
import com.wanari.tutelar.core.JwtService
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt.algorithms.{JwtAsymmetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._

import scala.concurrent.duration.Duration

class JwtServiceImpl[F[_]: MonadError[*[_], Throwable]](config: JwtConfig) extends JwtService[F] {

  import cats.syntax.functor._
  import cats.syntax.flatMap._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  protected implicit val clock = Clock.systemDefaultZone()

  private def settings: F[Settings] = {
    val expirationTime = config.expirationTime.toSeconds
    JwtAlgorithm
      .optionFromString(config.algorithm)
      .collect {
        case algo: JwtHmacAlgorithm       => Settings(algo, config.secret, config.secret, expirationTime)
        case algo: JwtAsymmetricAlgorithm => Settings(algo, config.privateKey, config.publicKey, expirationTime)
      }
      .pureOrRaise(WrongConfig("Unsupported JWT algorithm"))
  }

  override def init: F[Unit] = {
    Security.addProvider(new BouncyCastleProvider)
    for {
      ec <- encode(JsObject())
      de <- decode(ec).value
      _  <- de.isDefined.pureUnitOrRise(WrongConfig("JWT can't decode after encode"))
    } yield ()
  }

  override def encode(data: JsObject, expirationTime: Option[Duration] = None): F[String] = {
    settings.map { set =>
      val expTime = expirationTime.map(_.toSeconds).getOrElse(set.expirationTime)
      val claim   = JwtClaim(data.compactPrint).expiresIn(expTime)
      JwtSprayJson.encode(claim, set.encodeKey, set.algo)
    }
  }

  private def decode(token: String): OptionT[F, JsObject] = {
    val result = settings.map { set =>
      set.algo match {
        case a: JwtHmacAlgorithm       => JwtSprayJson.decodeJson(token, set.decodeKey, Seq(a)).toOption
        case a: JwtAsymmetricAlgorithm => JwtSprayJson.decodeJson(token, set.decodeKey, Seq(a)).toOption
      }
    }
    OptionT(result)
  }

  override def validateAndDecode(token: String): ErrorOr[F, JsObject] = {
    for {
      _    <- EitherT.right(validate(token)).ensure(InvalidJwt())(identity)
      data <- decode(token).toRight[AppError](InvalidJwt())
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
