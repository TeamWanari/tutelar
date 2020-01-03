package com.wanari.tutelar.core.impl

import cats.MonadError
import cats.data.{EitherT, OptionT}
import com.wanari.tutelar.core.AuthService.{LongTermToken, ShortTermToken, TokenData}
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.Errors._
import com.wanari.tutelar.core._
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.{DateTimeUtil, IdGenerator}
import spray.json.{JsNumber, JsObject, JsString}

class AuthServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit databaseService: DatabaseService[F],
    hookService: HookService[F],
    idGenerator: IdGenerator[F],
    timeService: DateTimeUtil[F],
    getJwtConfig: String => JwtConfig
) extends AuthService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  protected val longTermTokenService: JwtService[F]  = new JwtServiceImpl[F](getJwtConfig("longTerm"))
  protected val shortTermTokenService: JwtService[F] = new JwtServiceImpl[F](getJwtConfig("shortTerm"))

  override def init: F[Unit] = {
    for {
      _ <- longTermTokenService.init
      _ <- shortTermTokenService.init
    } yield ()
  }

  override def registerOrLogin(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  )(implicit ctx: LogContext): ErrorOr[F, TokenData] = {
    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    val result = for {
      account_hookresponse <- createOrUpdateAccount(authType, standardizedExternalId, customData, providedData)
      (account, hookResponse) = account_hookresponse
      token <- createTokenData(account, hookResponse)
    } yield token
    EitherT.right(result)
  }

  override def findCustomData(authType: String, externalId: String): OptionT[F, String] = {
    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    OptionT(databaseService.findAccountByTypeAndExternalId((authType, standardizedExternalId))).map(_.customData)
  }

  override def deleteUser(userId: String)(implicit ctx: LogContext): ErrorOr[F, Unit] = {
    for {
      _ <- EitherT.fromOptionF(databaseService.findUserById(userId), UserNotFound())
      _ <- EitherT.right(databaseService.deleteUserWithAccountsById(userId))
      _ <- EitherT.right(hookService.delete(userId))
    } yield ()
  }

  override def findUserIdInShortTermToken(token: ShortTermToken): OptionT[F, String] = {
    for {
      decoded <- shortTermTokenService.validateAndDecode(token).toOption
      id      <- OptionT.fromOption(decoded.fields.get("id").collect { case JsString(id) => id })
    } yield id
  }

  override def link(
      userId: String,
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  )(implicit ctx: LogContext): ErrorOr[F, Unit] = {
    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    val account                = Account(authType, standardizedExternalId, userId, customData)
    for {
      _ <- EitherT
        .right(databaseService.findAccountByTypeAndExternalId((authType, standardizedExternalId)))
        .ensure(AccountUsed())(_.isEmpty)
      _ <- EitherT
        .right(databaseService.listAccountsByUserId(userId))
        .ensure(UserNotFound())(_.nonEmpty)
        .ensure(UserHadThisAccountType())(_.forall(_.authType != authType))
      _ <- EitherT.right(databaseService.saveAccount(account))
      _ <- EitherT.right(hookService.link(userId, standardizedExternalId, authType, providedData))
    } yield ()
  }

  override def unlink(userId: String, authType: String)(implicit ctx: LogContext): ErrorOr[F, Unit] = {
    for {
      accounts <- EitherT
        .right(databaseService.listAccountsByUserId(userId))
        .ensure(UserLastAccount())(_.size > 1)
      account <- EitherT.fromOption(accounts.find(_.authType == authType), AccountNotFound())
      _       <- EitherT.right(databaseService.deleteAccountByUserAndType(userId, authType))
      _       <- EitherT.right(hookService.unlink(userId, account.externalId, authType))
    } yield ()
  }

  private def createOrUpdateAccount(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  )(implicit ctx: LogContext): F[(Account, JsObject)] = {
    databaseService
      .findAccountByTypeAndExternalId((authType, externalId))
      .flatMap(
        _.fold(
          register(authType, externalId, customData, providedData)
        ) { account =>
          login(account, customData, providedData)
        }
      )
  }

  private def login(account: Account, customData: String, providedData: JsObject)(
      implicit ctx: LogContext
  ): F[(Account, JsObject)] = {
    for {
      _    <- databaseService.updateCustomData(account.getId, customData)
      data <- hookService.login(account.userId, account.externalId, account.authType, providedData)
    } yield (account, data)
  }

  private def register(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  )(implicit ctx: LogContext): F[(Account, JsObject)] = {
    for {
      id   <- idGenerator.generate()
      time <- timeService.getCurrentTimeMillis
      user    = User(id, time)
      account = Account(authType, externalId, user.id, customData)
      _    <- databaseService.saveUser(user)
      _    <- databaseService.saveAccount(account)
      data <- hookService.register(id, externalId, authType, providedData)
    } yield (account, data)
  }

  private def createTokenData(account: Account, extraData: JsObject): F[TokenData] = {
    for {
      sortData  <- createShortTermJwtData(account, extraData)
      longData  <- createLongTermTokenJwtData(account, extraData)
      shortTerm <- shortTermTokenService.encode(sortData)
      longTerm  <- longTermTokenService.encode(longData)
    } yield {
      TokenData(shortTerm, longTerm)
    }
  }

  private def createShortTermJwtData(account: Account, extraData: JsObject): F[JsObject] = {
    import com.wanari.tutelar.util.SpraySyntax._
    (extraData + ("id" -> JsString(account.userId))).pure
  }

  private def createLongTermTokenJwtData(account: Account, extraData: JsObject): F[JsObject] = {
    timeService.getCurrentTimeMillis.map { time =>
      import com.wanari.tutelar.util.SpraySyntax._
      val id        = "id"        -> JsString(account.userId)
      val createdAt = "createdAt" -> JsNumber(time)
      extraData + id + createdAt
    }
  }

  private def convertToStandardizedLowercase(s: String): String = {
    import java.text.Normalizer
    Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase
  }

  override def refreshToken(token: LongTermToken)(implicit ctx: LogContext): ErrorOr[F, TokenData] = {
    for {
      decoded <- longTermTokenService.validateAndDecode(token)
      userId <- EitherT.fromOption(
        decoded.fields.get("id").collect { case JsString(id) => id },
        InvalidTokenMissingId()
      )
      _         <- EitherT.fromOptionF(databaseService.findUserById(userId), UserNotFound())
      tokenData <- EitherT.rightT(JsObject(decoded.fields.view.filterKeys(_ != "exp").toList: _*))
      shortTerm <- EitherT.right(shortTermTokenService.encode(tokenData))
      longTerm  <- EitherT.right(longTermTokenService.encode(tokenData))
    } yield {
      TokenData(shortTerm, longTerm)
    }
  }
}
