package com.wanari.tutelar.core.impl

import cats.MonadError
import cats.data.OptionT
import com.wanari.tutelar.core.AuthService.{LongTermToken, ShortTermToken, TokenData}
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.Errors._
import com.wanari.tutelar.core._
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.{DateTimeUtil, IdGenerator}
import spray.json.{JsObject, JsString}

class AuthServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit databaseService: DatabaseService[F],
    hookService: HookService[F],
    idGenerator: IdGenerator[F],
    timeService: DateTimeUtil[F],
    getJwtConfig: String => JwtConfig
) extends AuthService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

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
  )(implicit ctx: LogContext): F[TokenData] = {
    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    for {
      account_hookresponse <- createOrUpdateAccount(authType, standardizedExternalId, customData, providedData)
      (account, hookResponse) = account_hookresponse
      token <- createTokenData(account, hookResponse)
    } yield token
  }

  override def findCustomData(authType: String, externalId: String): OptionT[F, String] = {
    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    OptionT(databaseService.findAccountByTypeAndExternalId((authType, standardizedExternalId))).map(_.customData)
  }

  override def deleteUser(userId: String)(implicit ctx: LogContext): F[Unit] = {
    for {
      user <- databaseService.findUserById(userId)
      _    <- user.nonEmpty.pureUnitOrRise(UserNotFound())
      _    <- databaseService.deleteUserWithAccountsById(userId)
      _    <- hookService.delete(userId)
    } yield ()
  }

  override def findUserIdInShortTermToken(token: ShortTermToken): OptionT[F, String] = {
    OptionT(for {
      decoded <- shortTermTokenService.validateAndDecode(token)
    } yield {
      decoded.fields.get("id").collect { case JsString(id) => id }
    })
  }

  override def link(
      userId: String,
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  )(implicit ctx: LogContext): F[Unit] = {
    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    val account                = Account(authType, standardizedExternalId, userId, customData)
    for {
      accountO <- databaseService.findAccountByTypeAndExternalId((authType, standardizedExternalId))
      _        <- accountO.isEmpty.pureUnitOrRise(AccountUsed())
      accounts <- databaseService.listAccountsByUserId(userId)
      _        <- accounts.nonEmpty.pureUnitOrRise(UserNotFound())
      _        <- accounts.exists(_.authType != authType).pureUnitOrRise(UserHadThisAccountType())
      _        <- databaseService.saveAccount(account)
      _        <- hookService.link(userId, standardizedExternalId, authType, providedData)
    } yield ()
  }

  override def unlink(userId: String, authType: String)(implicit ctx: LogContext): F[Unit] = {
    for {
      accounts <- databaseService.listAccountsByUserId(userId)
      _        <- (accounts.size > 1).pureUnitOrRise(UserLastAccount())
      account  <- accounts.find(_.authType == authType).pureOrRaise(AccountNotFound())
      _        <- databaseService.deleteAccountByUserAndType(userId, authType)
      _        <- hookService.unlink(userId, account.externalId, authType)
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
      time <- timeService.getCurrentTimeMillis()
      user    = User(id, time)
      account = Account(authType, externalId, user.id, customData)
      _    <- databaseService.saveUser(user)
      _    <- databaseService.saveAccount(account)
      data <- hookService.register(id, externalId, authType, providedData)
    } yield (account, data)
  }

  private def createTokenData(account: Account, extraData: JsObject): F[TokenData] = {
    for {
      data      <- createJwtData(account, extraData)
      shortTerm <- shortTermTokenService.encode(data)
      longTerm  <- longTermTokenService.encode(data)
    } yield {
      TokenData(shortTerm, longTerm)
    }
  }

  private def createJwtData(account: Account, extraData: JsObject): F[JsObject] = {
    import com.wanari.tutelar.util.SpraySyntax._
    (extraData + ("id" -> JsString(account.userId))).pure
  }

  private def convertToStandardizedLowercase(s: String): String = {
    import java.text.Normalizer
    Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase
  }

  override def refreshToken(token: LongTermToken)(implicit ctx: LogContext): OptionT[F, TokenData] = {
    for {
      decoded   <- OptionT.liftF(longTermTokenService.validateAndDecode(token))
      userId    <- OptionT.fromOption(decoded.fields.get("id").collect { case JsString(id) => id })
      _         <- OptionT(databaseService.findUserById(userId))
      tokenData <- OptionT.some(JsObject(decoded.fields.view.filterKeys(_ != "exp").toList: _*))
      shortTerm <- OptionT.liftF(shortTermTokenService.encode(tokenData))
      longTerm  <- OptionT.liftF(longTermTokenService.encode(tokenData))
    } yield {
      TokenData(shortTerm, longTerm)
    }
  }
}
