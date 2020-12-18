package com.wanari.tutelar.core.impl

import cats.MonadError
import cats.data.{EitherT, OptionT}
import com.wanari.tutelar.core.AuthService.{LongTermToken, ShortTermToken, TokenData}
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.Errors._
import com.wanari.tutelar.core._
import com.wanari.tutelar.core.impl.AuthServiceImpl._
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.{DateTimeUtil, IdGenerator}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.Try

class AuthServiceImpl[F[_]: MonadError[*[_], Throwable]](implicit
    databaseService: DatabaseService[F],
    hookService: HookService[F],
    idGenerator: IdGenerator[F],
    timeService: DateTimeUtil[F],
    expirationService: ExpirationService[F],
    getJwtConfig: String => JwtConfig
) extends AuthService[F] {
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

    val refreshTokenDataEO: ErrorOr[F, Option[LongTermTokenData]] = refreshToken match {
      case None    => EitherT.rightT(Option.empty)
      case Some(t) => parseRefreshToken(t)
    }

    val standardizedExternalId = convertToStandardizedLowercase(externalId)
    for {
      tokenDataO <- refreshTokenDataEO
      account_hookresponse <- createOrUpdateAccount(
        authType,
        standardizedExternalId,
        customData,
        providedData,
        tokenDataO.map(_.id)
      )
      (account, hookResponse) = account_hookresponse
      token <- createTokenDataForAuthenticated(account, hookResponse, tokenDataO)
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
      _ <-
        EitherT
          .right(databaseService.findAccountByTypeAndExternalId((authType, standardizedExternalId)))
          .ensure(AccountUsed())(_.isEmpty)
      _ <-
        EitherT
          .right(databaseService.listAccountsByUserId(userId))
          .ensure(UserNotFound())(_.nonEmpty)
          .ensure(UserHadThisAccountType())(_.forall(_.authType != authType))
      _    <- EitherT.right(databaseService.saveAccount(account))
      data <- EitherT.right(hookService.link(userId, standardizedExternalId, authType, providedData))
    } yield (account, data)
  }

  override def unlink(userId: String, authType: String)(implicit ctx: LogContext): ErrorOr[F, Unit] = {
    for {
      accounts <-
        EitherT
          .right(databaseService.listAccountsByUserId(userId))
          .ensure(UserLastAccount())(_.size > 1)
      account <- EitherT.fromOption(accounts.find(_.authType == authType), AccountNotFound())
      _       <- EitherT.right(databaseService.deleteAccountByUserAndType(userId, authType))
      _       <- EitherT.right(hookService.unlink(userId, account.externalId, authType))
    } yield ()
  }

  override def refreshToken(token: LongTermToken)(implicit ctx: LogContext): ErrorOr[F, TokenData] = {
    for {
      refreshTokenO   <- parseRefreshToken(token)
      refreshToken    <- EitherT.fromOption(refreshTokenO, InvalidToken())
      _               <- EitherT.fromOptionF(databaseService.findUserById(refreshToken.id), UserNotFound())
      hookData        <- EitherT.right(hookService.refreshToken(refreshToken.id, refreshToken.data))
      newRefreshToken <- createNewRefreshToken(refreshToken, hookData)
      tokenData       <- EitherT.right(convertToTokenData(newRefreshToken))
    } yield tokenData
  }

  override def findProviderCustomDataByUserId(userId: String, authType: String)(implicit
      ctx: LogContext
  ): OptionT[F, String] = {
    val standardizedUserId = convertToStandardizedLowercase(userId)
    for {
      accs <- OptionT.liftF(databaseService.listAccountsByUserId(standardizedUserId))
      acc  <- OptionT.fromOption(accs.find(_.authType.toLowerCase.equals(authType.toLowerCase)))
    } yield acc.customData
  }

  private def createNewRefreshToken(
      data: LongTermTokenData,
      hookData: JsObject
  )(implicit ctx: LogContext): ErrorOr[F, LongTermTokenData] = {
    for {
      token <- removeExpiredProviders(data)
      data  <- combinedJsonDataWithHookData(data.data, hookData)
      time  <- EitherT.right[AppError](timeService.getCurrentTimeMillis)
    } yield token.copy(createdAt = time, data = data)
  }

  private def combinedJsonDataWithHookData(oldData: JsObject, hookData: JsObject): ErrorOr[F, JsObject] = {
    if (hookData.fields.isEmpty) {
      EitherT.pure(oldData)
    } else {
      EitherT.pure(hookData)
    }
  }

  private def removeExpiredProviders(
      data: LongTermTokenData
  )(implicit ctx: LogContext): ErrorOr[F, LongTermTokenData] = {
    import cats.syntax.traverse._

    val providersF = data.providers.toList
      .traverse[F, (Boolean, ProviderData)] { provider: ProviderData =>
        expirationService.isExpired(provider.name, data.createdAt, provider.loginAt).map(_ -> provider)
      }
      .map(_.collect { case (false, provider) =>
        provider
      })

    for {
      providers <- EitherT.right(providersF).ensure(LoginExpired(): AppError)(_.nonEmpty)
    } yield {
      data.copy(providers = providers)
    }
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

  private def login(account: Account, customData: String, providedData: JsObject)(implicit
      ctx: LogContext
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

  private def createTokenDataForAuthenticated(
      account: Account,
      hookData: JsObject,
      refreshTokenData: Option[LongTermTokenData]
  ): ErrorOr[F, TokenData] = {
    val result = for {
      longData <- createLongTermTokenJwtDataForAuthenticated(
        account.authType,
        account.userId,
        hookData,
        refreshTokenData
      )
      tokenData <- convertToTokenData(longData)
    } yield tokenData
    EitherT.right(result)
  }

  private def createLongTermTokenJwtDataForAuthenticated(
      authType: String,
      userId: String,
      hookData: JsObject,
      refreshTokenData: Option[LongTermTokenData]
  ): F[LongTermTokenData] = {
    timeService.getCurrentTimeMillis.map { time =>
      val providers = refreshTokenData
        .map(_.providers)
        .getOrElse(Seq.empty)
        .filterNot(_.name == authType) :+ ProviderData(authType, time)

      LongTermTokenData(userId, time, providers, hookData)
    }
  }

  private def convertToStandardizedLowercase(s: String): String = {
    import java.text.Normalizer
    Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase
  }

  private def parseRefreshToken(token: String): ErrorOr[F, Option[LongTermTokenData]] = {
    longTermTokenService
      .validateAndDecode(token)
      .map(data => Try(data.convertTo[LongTermTokenData]).toOption)
  }

  private def convertToTokenData(longTermTokenData: LongTermTokenData): F[TokenData] = {
    for {
      shortTerm <- shortTermTokenService.encode(longTermTokenData.asShortTermToken)
      longTerm  <- longTermTokenService.encode(longTermTokenData.toJson.asJsObject)
    } yield {
      TokenData(shortTerm, longTerm)
    }
  }
}

object AuthServiceImpl {
  private case class ShortTermTokenBaseData(id: String, providers: Seq[String])
  private case class LongTermTokenData(id: String, createdAt: Long, providers: Seq[ProviderData], data: JsObject) {
    def asShortTermToken: JsObject = {
      val baseData = ShortTermTokenBaseData(
        id,
        providers.map(_.name)
      ).toJson.asJsObject

      JsObject(data.fields ++ baseData.fields)
    }
  }
  private case class ProviderData(name: String, loginAt: Long)
  private implicit val formatterProviderData: RootJsonFormat[ProviderData] = jsonFormat2(ProviderData)
  private implicit val formatterShortTermTokenBaseData: RootJsonFormat[ShortTermTokenBaseData] = jsonFormat2(
    ShortTermTokenBaseData
  )
  private implicit val formatterLongTermTokenData: RootJsonFormat[LongTermTokenData] = jsonFormat4(LongTermTokenData)
}
