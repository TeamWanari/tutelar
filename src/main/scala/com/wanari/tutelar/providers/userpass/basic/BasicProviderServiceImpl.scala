package com.wanari.tutelar.providers.userpass.basic
import cats.MonadError
import cats.data.EitherT
import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors._
import com.wanari.tutelar.providers.userpass.PasswordDifficultyChecker
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.PasswordCryptor
import spray.json.JsObject

class BasicProviderServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit authService: AuthService[F],
    passwordDifficultyChecker: PasswordDifficultyChecker[F]
) extends BasicProviderService[F]
    with PasswordCryptor {
  protected val authType = "BASIC"

  override def register(username: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      _     <- EitherT.right(passwordDifficultyChecker.isValid(password)).ensure(WeakPassword())(identity)
      _     <- authService.findCustomData(authType, username).toLeft(()).leftMap(_ => UsernameUsed())
      token <- authService.registerOrLogin(authType, username, encryptPassword(password), data.getOrElse(JsObject()))
    } yield token
  }

  override def login(username: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      passwordHash <- authService
        .findCustomData(authType, username)
        .toRight[AppError](UserNotFound())
        .ensure(AuthenticationFailed())(hash => checkPassword(password, hash))
      token <- authService.registerOrLogin(authType, username, passwordHash, data.getOrElse(JsObject()))
    } yield token
  }
}
