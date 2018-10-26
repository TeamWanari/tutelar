package com.wanari.tutelar

import cats.Applicative
import com.wanari.tutelar.DatabaseService.{Account, User}

import scala.collection.mutable

class DatabaseServiceMemImpl[F[_]: Applicative] extends DatabaseService[F] {
  import cats.syntax.applicative._

  val users    = mutable.Map.empty[String, User]
  val accounts = mutable.Map.empty[(String, String), Account]

  override def saveUser(user: DatabaseService.User): F[Unit] = { users += (user.id -> user); () }.pure

  override def saveAccount(account: DatabaseService.Account): F[Unit] = {
    accounts += ((account.authType, account.externalId) -> account); ()
  }.pure

  override def findUserById(id: String): F[Option[DatabaseService.User]] = users.get(id).pure

  override def findAccountByTypeAndExternalId(
      authType: String,
      externalId: String
  ): F[Option[DatabaseService.Account]] = accounts.get((authType, externalId)).pure

  override def listAccountsByUserId(userId: String): F[Seq[DatabaseService.Account]] =
    accounts.values.filter(_.userId == userId).toSeq.pure

  override def checkStatus(): F[Boolean] = true.pure
}
