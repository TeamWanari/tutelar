package com.wanari.tutelar.core.impl.database

import cats.MonadError
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.DatabaseService.AccountId
import com.wanari.tutelar.core.impl.database.DatabaseServiceProxy.DatabaseServiceProxyConfig

class DatabaseServiceProxy[F[_]: MonadError[?[_], Throwable]](services: Map[String, () => DatabaseService[F]])(
    implicit config: () => F[DatabaseServiceProxyConfig]
) extends DatabaseService[F] {
  import cats.syntax.flatMap._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  protected lazy val service: F[DatabaseService[F]] = {
    config().flatMap { config =>
      services.get(config.database.toLowerCase).map(_()).pureOrRaise(new Exception("Not supported database service = "))
    }
  }

  override def init: F[Unit] =
    service.flatMap(_.init)

  override def checkStatus(): F[Boolean] =
    service.flatMap(_.checkStatus())

  override def saveUser(user: DatabaseService.User): F[Unit] =
    service.flatMap(_.saveUser(user))

  override def saveAccount(account: DatabaseService.Account): F[Unit] =
    service.flatMap(_.saveAccount(account))

  override def findUserById(id: String): F[Option[DatabaseService.User]] =
    service.flatMap(_.findUserById(id))

  override def findAccountByTypeAndExternalId(accountId: AccountId): F[Option[DatabaseService.Account]] =
    service.flatMap(_.findAccountByTypeAndExternalId(accountId))

  override def listAccountsByUserId(userId: String): F[Seq[DatabaseService.Account]] =
    service.flatMap(_.listAccountsByUserId(userId))

  override def updateCustomData(accountId: AccountId, customData: String): F[Unit] =
    service.flatMap(_.updateCustomData(accountId, customData))

  override def deleteUserWithAccountsById(userId: String): F[Unit] =
    service.flatMap(_.deleteUserWithAccountsById(userId))
}

object DatabaseServiceProxy {
  case class DatabaseServiceProxyConfig(database: String)
  object DatabaseServiceProxyConfig {
    val MEMORY   = "memory"
    val POSTGRES = "postgres"
  }
}
