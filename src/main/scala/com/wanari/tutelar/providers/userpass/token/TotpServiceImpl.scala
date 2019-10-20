package com.wanari.tutelar.providers.userpass.token

import java.security.SecureRandom

import cats.MonadError
import cats.data.EitherT
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors.{ErrorOr, InvalidAlgo, UserNotFound, UsernameUsed, WrongPassword}
import com.wanari.tutelar.core.impl.JwtServiceImpl
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{AuthService, JwtService}
import com.wanari.tutelar.providers.userpass.token.OTP.{OTPAlgorithm, OTPKey, TOTP}
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.RootJsonFormat

import scala.util.Try

class TotpServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit authService: AuthService[F],
    config: TotpConfig,
    getJwtConfig: String => JwtConfig
) extends TotpService[F] {
  import TotpServiceImpl._
  import spray.json._

  protected val authType                   = "TOTP"
  protected def now: Long                  = System.currentTimeMillis / 1000
  protected val secureRandom: SecureRandom = OTPKey.defaultPRNG

  protected val jwtService: JwtService[F] = new JwtServiceImpl[F](getJwtConfig("totpProvider"))

  override def init: F[Unit] = {
    jwtService.init
  }

  override def qrCodeData: ErrorOr[F, QRData] = {
    for {
      algo <- EitherT.fromOption(OTPAlgorithm.algos.find(_.name == config.algorithm), InvalidAlgo(config.algorithm))
      key      = OTPKey.randomStrong(algo, secureRandom)
      totp     = TOTP(algo, config.digits, config.period, if (config.startFromCurrentTime) now else 0, key)
      totpData = TotpData(config.algorithm, totp.digits, totp.period, totp.initialTimestamp, key.toBase32)
      uri      = totp.toURI("")
      token <- EitherT.right(jwtService.encode(totpData.toJson.asJsObject))
    } yield {
      QRData(token, uri)
    }
  }

  override def register(
      userName: String,
      registerToken: String,
      password: String,
      data: Option[JsObject],
      refreshToken: Option[LongTermToken]
  )(
      implicit ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      totpDataAsString <- decodeToken(registerToken)
      _                <- checkPassword(totpDataAsString, password, config.window)
      _                <- authService.findCustomData(authType, userName).toLeft(()).leftMap(_ => UsernameUsed())
      token <- authService.authenticatedWith(
        authType,
        userName,
        totpDataAsString,
        data.getOrElse(JsObject()),
        refreshToken
      )
    } yield token
  }

  override def login(username: String, password: String, data: Option[JsObject], refreshToken: Option[LongTermToken])(
      implicit ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      savedData <- authService.findCustomData(authType, username).toRight(UserNotFound())
      _         <- checkPassword(savedData, password, config.window)
      token     <- authService.authenticatedWith(authType, username, savedData, data.getOrElse(JsObject()), refreshToken)
    } yield token
  }

  private def checkPassword(savedData: String, recievedToken: String, window: Int): ErrorOr[F, Unit] = {
    val totpResponse = for {
      data <- Try(savedData.parseJson.convertTo[TotpData]).toOption
      algo <- OTPAlgorithm.algos.find(_.name == data.algorithm)
      key  <- Try(OTPKey(data.otpkey)).toOption
      totp = TOTP(algo, data.digits, data.period, data.initialTimestamp, key)
      matchingTimestamp <- totp.validate(now, window)(recievedToken)
    } yield {
      ()
    }
    EitherT.fromOption(totpResponse, WrongPassword())
  }

  private def decodeToken(str: String): ErrorOr[F, String] = {
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
