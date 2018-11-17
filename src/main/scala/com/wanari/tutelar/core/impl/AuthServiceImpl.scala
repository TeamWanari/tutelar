package com.wanari.tutelar.core.impl

import cats.Monad
import com.wanari.tutelar.core.AuthService.{AuthConfig, CallbackUrl}
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.{AuthService, DatabaseService, HookService, JwtService}
import com.wanari.tutelar.util.{DateTimeUtil, IdGenerator}
import spray.json.{JsObject, JsString}

class AuthServiceImpl[F[_]: Monad](
    implicit databaseService: DatabaseService[F],
    hookService: HookService[F],
    idGenerator: IdGenerator[F],
    timeService: DateTimeUtil[F],
    jwtService: F[JwtService[F]],
    authConfig: () => F[AuthConfig]
) extends AuthService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  override def registerOrLogin(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  ): F[CallbackUrl] = {
    for {
      account_hookresponse <- createOrUpdateAccount(authType, externalId, customData, providedData)
      (account, hookResponse) = account_hookresponse
      token <- createJwt(account, hookResponse)
      url   <- createCallbackUrl(token)
    } yield {
      url
    }
  }

  private def createOrUpdateAccount(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  ): F[(Account, JsObject)] = {
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

  private def login(account: Account, customData: String, providedData: JsObject): F[(Account, JsObject)] = {
    for {
      _    <- databaseService.updateCustomData(account.getId, customData)
      data <- hookService.login(account.userId, account.authType, providedData)
    } yield (account, data)
  }

  private def register(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject
  ): F[(Account, JsObject)] = {
    for {
      id   <- idGenerator.generate()
      time <- timeService.getCurrentTimeMillis()
      user    = User(id, time)
      account = Account(authType, externalId, user.id, customData)
      _    <- databaseService.saveUser(user)
      _    <- databaseService.saveAccount(account)
      data <- hookService.register(id, authType, providedData)
    } yield (account, data)
  }

  private def createJwt(account: Account, extraData: JsObject): F[String] = {
    for {
      data    <- createJwtData(account, extraData)
      service <- jwtService
      jwt     <- service.encode(data)
    } yield {
      jwt
    }
  }

  private def createJwtData(account: Account, extraData: JsObject): F[JsObject] = {
    import com.wanari.tutelar.util.SpraySyntax._
    (extraData + ("id" -> JsString(account.userId))).pure
  }

  private def createCallbackUrl(token: String): F[CallbackUrl] = {
    authConfig().map(_.callback.replace("<<TOKEN>>", token))
  }
}
