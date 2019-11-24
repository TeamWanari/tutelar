package com.wanari.tutelar.core

import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.DatabaseService.{Account, AccountId, User, UserIdWithExternalId}

trait DatabaseService[F[_]] extends Initable[F] {
  def checkStatus(): F[Boolean]
  def saveUser(user: User): F[Unit]
  def saveAccount(account: Account): F[Unit]
  def findUserById(id: String): F[Option[User]]
  def findAccountByTypeAndExternalId(accountId: AccountId): F[Option[Account]]
  def listAccountsByUserId(userId: String): F[Seq[Account]]
  def listUserIdsByAuthType(authType: String): F[Seq[UserIdWithExternalId]]
  def updateCustomData(accountId: AccountId, customData: String): F[Unit]
  def deleteUserWithAccountsById(userId: String): F[Unit]
  def deleteAccountByUserAndType(userId: String, authType: String): F[Unit]
}

object DatabaseService {
  type AccountId = (String, String)
  case class User(id: String, createdAt: Long)
  case class Account(authType: String, externalId: String, userId: String, customData: String) {
    def getId: AccountId = (authType, externalId)
  }
  case class UserIdWithExternalId(userId: String, externalId: String)
}
