package com.wanari.tutelar.providers.userpass.token

import java.security.SecureRandom

import cats.MonadError
import cats.data.OptionT
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{AuthService, JwtService}
import com.wanari.tutelar.providers.userpass.token.OTP.{OTPAlgorithm, OTPKey, TOTP}
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.RootJsonFormat

import scala.util.Try

class TotpServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit authService: AuthService[F],
    totpConfig: () => F[TotpConfig],
    getJwtConfig: String => JwtConfig
) extends TotpService[F] {
  import TotpServiceImpl._
  import cats.implicits._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._
  import spray.json._

  protected val authType                   = "TOTP"
  protected def now: Long                  = System.currentTimeMillis / 1000
  protected val secureRandom: SecureRandom = OTPKey.defaultPRNG

  protected val jwtService: JwtService[F] = new JwtServiceImpl[F](getJwtConfig("totpProvider"))

  override def init: F[Unit] = {
    jwtService.init
  }

  override def qrCodeData: F[QRData] = {
    val result = for {
      config <- OptionT.liftF(totpConfig())
      algo   <- OptionT.fromOption[F](OTPAlgorithm.algos.find(_.name == config.algorithm))
      key      = OTPKey.randomStrong(algo, secureRandom)
      totp     = TOTP(algo, config.digits, config.period, if (config.startFromCurrentTime) now else 0, key)
      totpData = TotpData(config.algorithm, totp.digits, totp.period, totp.initialTimestamp, key.toBase32)
      uri      = totp.toURI("")
      token <- OptionT.liftF(jwtService.encode(totpData.toJson.asJsObject))
    } yield {
      QRData(token, uri)
    }

    result.pureOrRaise(new Exception())
  }

  override def register(userName: String, registerToken: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[TokenData] = {
    val result = for {
      config           <- OptionT.liftF(totpConfig())
      totpDataAsString <- OptionT.liftF(decodeToken(registerToken))
      _                <- checkPassword(totpDataAsString, password, config.window)
      usernameIsUsed   <- OptionT.liftF(authService.findCustomData(authType, userName).isDefined) if !usernameIsUsed
      token <- OptionT.liftF(
        authService.registerOrLogin(authType, userName, totpDataAsString, data.getOrElse(JsObject()))
      )
    } yield token

    result.pureOrRaise(new Exception())
  }

  override def login(username: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[TokenData] = {
    val result: OptionT[F, TokenData] = for {
      config    <- OptionT.liftF(totpConfig())
      savedData <- authService.findCustomData(authType, username)
      _         <- checkPassword(savedData, password, config.window)
      token     <- OptionT.liftF(authService.registerOrLogin(authType, username, savedData, data.getOrElse(JsObject())))
    } yield token

    result.pureOrRaise(new Exception())
  }

  private def checkPassword(savedData: String, recievedToken: String, window: Int): OptionT[F, Unit] = {
    val totpResponse = for {
      data <- Try(savedData.parseJson.convertTo[TotpData]).toOption
      algo <- OTPAlgorithm.algos.find(_.name == data.algorithm)
      key  <- Try(OTPKey(data.otpkey)).toOption
      totp = TOTP(algo, data.digits, data.period, data.initialTimestamp, key)
      matchingTimestamp <- totp.validate(now, window)(recievedToken)
    } yield {
      ()
    }
    totpResponse.toOptionT[F]
  }

  private def decodeToken(str: String): F[String] = {
    jwtService.validateAndDecode(str).map(_.toString)
  }
}

object TotpServiceImpl {
  case class TotpData(algorithm: String, digits: Int, period: Int, initialTimestamp: Long, otpkey: String)
  case class QRData(token: String, uri: String)

  import spray.json.DefaultJsonProtocol._
  implicit val totpDataFormat: RootJsonFormat[TotpData] = jsonFormat5(TotpData)
  implicit val qrDataFormat: RootJsonFormat[QRData]     = jsonFormat2(QRData)

  case class TotpConfig(
      algorithm: String,
      window: Int,
      period: Int,
      digits: Int,
      startFromCurrentTime: Boolean
  )
}
