package com.wanari.tutelar
import cats.Monad
import com.wanari.tutelar.AuthService.CallbackUrl
import com.wanari.tutelar.DatabaseService.{Account, User}
import com.wanari.tutelar.jwt.JwtService
import spray.json.{JsObject, JsString}

trait AuthService[F[_]] {
  def registerOrLogin(authType: String, externalId: String, customData: String): F[CallbackUrl]
}

object AuthService {
  type CallbackUrl = String
}

class AuthServiceImpl[F[_]: Monad](
    implicit databaseService: DatabaseService[F],
    idGenerator: IdGenerator[F],
    timeService: DateTimeService[F],
    jwtService: F[JwtService[F]],
    authConfigService: AuthConfigService[F]
) extends AuthService[F] {
  import cats.syntax.functor._
  import cats.syntax.applicative._
  import cats.syntax.flatMap._

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
    authConfigService.getCallbackUrl.map(_.replace("<<TOKEN>>", token))
  }
}
