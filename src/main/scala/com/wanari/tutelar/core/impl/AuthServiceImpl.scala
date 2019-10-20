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

  override def authenticatedWith(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject,
      refreshToken: Option[LongTermToken]
  )(implicit ctx: LogContext): ErrorOr[F, TokenData] = {
    // TODO - check refreshToken (when defined)
    val userId: Option[String] = None // TODO: from refreshToken

    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    for {
      account_hookresponse <- createOrUpdateAccount(authType, standardizedExternalId, customData, providedData, userId)
      (account, hookResponse) = account_hookresponse
      token <- createTokenData(account, hookResponse) // TODO: use refreshToken data
    } yield token
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
  )(implicit ctx: LogContext): ErrorOr[F, (Account, JsObject)] = {
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
      _    <- EitherT.right(databaseService.saveAccount(account))
      data <- EitherT.right(hookService.link(userId, standardizedExternalId, authType, providedData))
    } yield (account, data)
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
      providedData: JsObject,
      userId: Option[String]
  )(implicit ctx: LogContext): ErrorOr[F, (Account, JsObject)] = {
    EitherT
      .right(databaseService.findAccountByTypeAndExternalId((authType, externalId)))
      .flatMap { mbAccount =>
        (mbAccount, userId) match {
          case (Some(acc), Some(id)) if acc.userId != id => EitherT.leftT(AccountBelongsToOtherUser())
          case (Some(acc), _)                            => login(acc, customData, providedData)
          case (None, None)                              => register(authType, externalId, customData, providedData)
          case (None, Some(id))                          => link(id, authType, externalId, customData, providedData)
        }
      }
  }

  private def login(account: Account, customData: String, providedData: JsObject)(
      implicit ctx: LogContext
  ): ErrorOr[F, (Account, JsObject)] = {
    val result = for {
      _    <- databaseService.updateCustomData(account.getId, customData)
      data <- hookService.login(account.userId, account.externalId, account.authType, providedData)
    } yield (account, data)
    EitherT.right(result)
  }

  private def register(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  )(implicit ctx: LogContext): ErrorOr[F, (Account, JsObject)] = {
    val result = for {
      id   <- idGenerator.generate()
      time <- timeService.getCurrentTimeMillis
      user    = User(id, time)
      account = Account(authType, externalId, user.id, customData)
      _    <- databaseService.saveUser(user)
      _    <- databaseService.saveAccount(account)
      data <- hookService.register(id, externalId, authType, providedData)
    } yield (account, data)
    EitherT.right(result)
  }

  private def createTokenData(account: Account, extraData: JsObject): ErrorOr[F, TokenData] = {
    val result = for {
      sortData  <- createShortTermJwtData(account.userId, extraData)
      longData  <- createLongTermTokenJwtData(account.userId, extraData)
      shortTerm <- shortTermTokenService.encode(sortData)
      longTerm  <- longTermTokenService.encode(longData)
    } yield {
      TokenData(shortTerm, longTerm)
    }
    EitherT.right(result)
  }

  private def createShortTermJwtData(userId: String, extraData: JsObject): F[JsObject] = {
    import com.wanari.tutelar.util.SpraySyntax._
    (extraData + ("id" -> JsString(userId))).pure
  }

  private def createLongTermTokenJwtData(userId: String, extraData: JsObject): F[JsObject] = {
    timeService.getCurrentTimeMillis.map { time =>
      import com.wanari.tutelar.util.SpraySyntax._
      val id        = "id"        -> JsString(userId)
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
      _ <- EitherT.fromOptionF(databaseService.findUserById(userId), UserNotFound())
      tokenData <- EitherT.rightT(
        JsObject(decoded.fields.view.filterKeys(_ != "exp").filterKeys(_ != "createdAt").toList: _*)
      )
      sortData  <- EitherT.right(createShortTermJwtData(userId, tokenData))
      longData  <- EitherT.right(createLongTermTokenJwtData(userId, tokenData))
      shortTerm <- EitherT.right(shortTermTokenService.encode(sortData))
      longTerm  <- EitherT.right(longTermTokenService.encode(longData))
    } yield {
      TokenData(shortTerm, longTerm)
    }
  }
}
