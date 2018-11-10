package com.wanari.tutelar.core.impl

import cats.Monad
import com.wanari.tutelar.core.AuthService.{AuthConfig, CallbackUrl}
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.{AuthService, DatabaseService, JwtService}
import com.wanari.tutelar.util.{DateTimeUtil, IdGenerator}
import spray.json.{JsObject, JsString}

class AuthServiceImpl[F[_]: Monad](
    implicit databaseService: DatabaseService[F],
    idGenerator: IdGenerator[F],
    timeService: DateTimeUtil[F],
    jwtService: F[JwtService[F]],
    authConfig: () => F[AuthConfig]
) extends AuthService[F] {
  import cats.syntax.applicative._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  override def registerOrLogin(authType: String, externalId: String, customData: String): F[CallbackUrl] = {
    for {
      account <- createOrUpdateAccount(authType, externalId, customData)
      token   <- createJwt(account)
      url     <- createCallbackUrl(token)
    } yield {
      url
    }
  }

  private def createOrUpdateAccount(authType: String, externalId: String, customData: String): F[Account] = {
    databaseService
      .findAccountByTypeAndExternalId((authType, externalId))
      .flatMap(
        _.fold(
          register(authType, externalId, customData)
        ) { account =>
          updateCustomData(account, customData).map(_ => account)
        }
      )
  }

  private def updateCustomData(account: Account, customData: String): F[Unit] = {
    databaseService.updateCustomData(account.getId, customData)
  }

  private def register(authType: String, externalId: String, customData: String): F[Account] = {
    for {
      id   <- idGenerator.generate()
      time <- timeService.getCurrentTimeMillis()
      user    = User(id, time)
      account = Account(authType, externalId, user.id, customData)
      _ <- databaseService.saveUser(user)
      _ <- databaseService.saveAccount(account)
    } yield {
      account
    }
  }

  private def createJwt(account: Account): F[String] = {
    for {
      data    <- createJwtData(account)
      service <- jwtService
      jwt     <- service.encode(data)
    } yield {
      jwt
    }
  }

  private def createJwtData(account: Account): F[JsObject] = {
    JsObject(
      "id" -> JsString(account.userId)
    ).pure
  }

  private def createCallbackUrl(token: String): F[CallbackUrl] = {
    authConfig().map(_.callback.replace("<<TOKEN>>", token))
  }
}
