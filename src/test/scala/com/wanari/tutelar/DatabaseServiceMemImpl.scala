package com.wanari.tutelar

import cats.Applicative
import com.wanari.tutelar.DatabaseService.{Account, AccountId, User}

import scala.collection.mutable

class DatabaseServiceMemImpl[F[_]: Applicative] extends DatabaseService[F] {
  import cats.syntax.applicative._

  val users    = mutable.Map.empty[String, User]
  val accounts = mutable.Map.empty[AccountId, Account]

  override def saveUser(user: DatabaseService.User): F[Unit] = { users += (user.id -> user); () }.pure

  override def saveAccount(account: DatabaseService.Account): F[Unit] = {
    accounts += ((account.authType, account.externalId) -> account); ()
  }.pure

  override def findUserById(id: String): F[Option[DatabaseService.User]] = users.get(id).pure

  override def findAccountByTypeAndExternalId(accountId: AccountId): F[Option[DatabaseService.Account]] =
    accounts.get(accountId).pure

  override def listAccountsByUserId(userId: String): F[Seq[DatabaseService.Account]] =
    accounts.values.filter(_.userId == userId).toSeq.pure

  override def updateCustomData(accountId: AccountId, customData: String): F[Unit] = {
    val account = accounts(accountId).copy(customData = customData)
    accounts.update(accountId, account).pure
  }

  override def checkStatus(): F[Boolean] = true.pure
}
