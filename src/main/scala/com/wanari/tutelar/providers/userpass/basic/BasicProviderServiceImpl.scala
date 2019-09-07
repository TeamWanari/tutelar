package com.wanari.tutelar.providers.userpass.basic
import cats.MonadError
import cats.data.OptionT
import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.{AuthenticationFailed, UsernameUsed}
import com.wanari.tutelar.providers.userpass.PasswordDifficultyChecker
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.PasswordCryptor
import spray.json.JsObject

class BasicProviderServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit authService: AuthService[F],
    passwordDifficultyChecker: PasswordDifficultyChecker[F]
) extends BasicProviderService[F]
    with PasswordCryptor {
  import cats.syntax.functor._
  import cats.syntax.flatMap._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  protected val authType = "BASIC"

  override def register(username: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[TokenData] = {
    for {
      _                 <- passwordDifficultyChecker.validate(password)
      usernameIsNotUsed <- authService.findCustomData(authType, username).isEmpty
      _                 <- usernameIsNotUsed.pureUnitOrRise(UsernameUsed())
      token             <- authService.registerOrLogin(authType, username, encryptPassword(password), data.getOrElse(JsObject()))
    } yield token
  }

  override def login(username: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[TokenData] = {
    val result: OptionT[F, TokenData] = for {
      passwordHash <- authService.findCustomData(authType, username) if checkPassword(password, passwordHash)
      token        <- OptionT.liftF(authService.registerOrLogin(authType, username, passwordHash, data.getOrElse(JsObject())))
    } yield token

    result.pureOrRaise(AuthenticationFailed())
  }
}
