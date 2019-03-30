package com.wanari.tutelar.core.impl

import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.DatabaseService.{Account, AccountId, User}

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}

class DatabaseServiceImpl(db: Database)(implicit ec: ExecutionContext) extends DatabaseService[Future] {

  override def init: Future[Unit] = {
    db.run(sql"SELECT 1".as[Int]).map(_ => ())
  }

  override def checkStatus(): Future[Boolean] = {
    db.run(sql"SELECT 1".as[Int])
      .map(_ => true)
      .recover { case _ => false }
  }

  override def saveUser(user: User): Future[Unit] = {
    val query = users += user
    db.run(query).map(_ => {})
  }

  override def saveAccount(account: Account): Future[Unit] = {
    val query = accounts += account
    db.run(query).map(_ => {})
  }

  override def findUserById(id: String): Future[Option[User]] = {
    val query = users
      .filter(_.id === id)
      .result
      .headOption
    db.run(query)
  }

  override def findAccountByTypeAndExternalId(accountId: AccountId): Future[Option[Account]] = {
    val query = accounts
      .filter(_.authType === accountId._1)
      .filter(_.externalId === accountId._2)
      .result
      .headOption
    db.run(query)
  }

  override def listAccountsByUserId(userId: String): Future[Seq[Account]] = {
    val query = accounts
      .filter(_.userId === userId)
      .result
    db.run(query)
  }

  override def updateCustomData(accountId: AccountId, customData: String): Future[Unit] = {
    val query = accounts
      .filter(_.authType === accountId._1)
      .filter(_.externalId === accountId._2)
      .map(_.customData)
      .update(customData)
    db.run(query).map(_ => {})
  }

  private lazy val users    = TableQuery[UsersTable]
  private lazy val accounts = TableQuery[AccountsTable]

  private class UsersTable(tag: Tag) extends Table[User](tag, "users") {
    def id: Rep[String]      = column[String]("id", O.PrimaryKey)
    def createdAt: Rep[Long] = column[Long]("created_at")

    def * = (id, createdAt) <> ((User.apply _).tupled, User.unapply)
  }
  private class AccountsTable(tag: Tag) extends Table[Account](tag, "accounts") {
    def authType: Rep[String]   = column[String]("auth_type", O.PrimaryKey)
    def externalId: Rep[String] = column[String]("external_id", O.PrimaryKey)
    def userId: Rep[String]     = column[String]("user_id")
    def customData: Rep[String] = column[String]("custom_data")

    def * = (authType, externalId, userId, customData) <> ((Account.apply _).tupled, Account.unapply)
  }

}

object DatabaseServiceImpl {
  //TODO this is an uncontrolled read from the config
  def getDatabase: Database = Database.forConfig("database")
}
