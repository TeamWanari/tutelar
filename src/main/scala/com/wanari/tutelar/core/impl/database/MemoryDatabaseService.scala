package com.wanari.tutelar.core.impl.database

import cats.Applicative
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.DatabaseService.{Account, AccountId, User}

class MemoryDatabaseService[F[_]: Applicative] extends DatabaseService[F] {
  import cats.syntax.applicative._
  import scala.jdk.CollectionConverters._

  val users    = new java.util.concurrent.ConcurrentHashMap[String, User].asScala
  val accounts = new java.util.concurrent.ConcurrentHashMap[AccountId, Account].asScala

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

  override def init: F[Unit] = ().pure

  override def deleteUserWithAccountsById(userId: String): F[Unit] = {
    users.remove(userId)
    accounts.filter(_._2.userId == userId).keys.foreach(accounts.remove).pure
  }

  override def deleteAccountByUserAndType(userId: String, authType: String): F[Unit] = {
    accounts
      .filter(_._2.userId == userId)
      .filter(_._2.authType == authType)
      .keys
      .foreach(accounts.remove)
      .pure
  }
}
