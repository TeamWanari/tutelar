package com.wanari.tutelar

import cats.Applicative
import com.wanari.tutelar.DataBaseService.{Account, User}

import scala.collection.mutable

class DataBaseServiceMemImpl[F[_]: Applicative] extends DataBaseService[F] {
  import cats.syntax.applicative._

  val users    = mutable.Map.empty[String, User]
  val accounts = mutable.Map.empty[(String, String), Account]

  override def saveUser(user: DataBaseService.User): F[Unit] = { users += (user.id -> user); () }.pure

  override def saveAccount(account: DataBaseService.Account): F[Unit] = {
    accounts += ((account.authType, account.externalId) -> account); ()
  }.pure

  override def findUserById(id: String): F[Option[DataBaseService.User]] = users.get(id).pure

  override def findAccountByTypeAndExternalId(
      authType: String,
      externalId: String
  ): F[Option[DataBaseService.Account]] = accounts.get((authType, externalId)).pure

  override def listAccountsByUserId(userId: String): F[Seq[DataBaseService.Account]] =
    accounts.values.filter(_.userId == userId).toSeq.pure

  override def checkStatus(): F[Boolean] = true.pure
}
