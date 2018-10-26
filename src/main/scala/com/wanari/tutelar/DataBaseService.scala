package com.wanari.tutelar

import com.wanari.tutelar.DataBaseService.{Account, User}

import scala.concurrent.{ExecutionContext, Future}

class DataBaseServiceImpl(implicit configService: ConfigService[Future], ec: ExecutionContext)
    extends DataBaseService[Future] {
  import slick.jdbc.PostgresProfile.api._

  lazy val dbf = configService.getDbUrl.map(
    url =>
      Database.forURL(
        url,
        driver = "org.postgresql.Driver",
        executor = AsyncExecutor("slick-executor", numThreads = 10, queueSize = 1000)
      )
  )

  lazy val users    = TableQuery[UsersTable]
  lazy val accounts = TableQuery[AccountsTable]

  class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id: Rep[String]      = column[String]("id", O.PrimaryKey)
    def createdAt: Rep[Long] = column[Long]("created_at")

    def * = (id, createdAt) <> ((User.apply _).tupled, User.unapply)
  }
  class AccountsTable(tag: Tag) extends Table[Account](tag, "ACCOUNTS") {
    def authType: Rep[String]   = column[String]("auth_type", O.PrimaryKey)
    def externalId: Rep[String] = column[String]("external_id", O.PrimaryKey)
    def userId: Rep[String]     = column[String]("user_id")
    def customData: Rep[String] = column[String]("custom_data")

    def * = (authType, externalId, userId, customData) <> ((Account.apply _).tupled, Account.unapply)
  }

  private def run[T](query: DBIO[T]): Future[T] = {
    dbf.flatMap(_.run(query))
  }

  override def checkStatus(): Future[Boolean] = {
    run(sql"SELECT 1".as[Int])
      .map(_ => true)
      .recover { case _ => false }
  }

  override def saveUser(user: User): Future[Unit] = {
    val query = users += user
    run(query).map(_ => {})
  }

  override def saveAccount(account: Account): Future[Unit] = {
    val query = accounts += account
    run(query).map(_ => {})
  }

  override def findUserById(id: String): Future[Option[User]] = {
    val query = users
      .filter(_.id === id)
      .result
      .headOption
    run(query)
  }

  override def findAccountByTypeAndExternalId(authType: String, externalId: String): Future[Option[Account]] = {
    val query = accounts
      .filter(_.authType === authType)
      .filter(_.externalId === externalId)
      .result
      .headOption
    run(query)
  }

  override def listAccountsByUserId(userId: String): Future[Seq[Account]] = {
    val query = accounts
      .filter(_.userId === userId)
      .result
    run(query)
  }

}

trait DataBaseService[F[_]] {
  def checkStatus(): F[Boolean]
  def saveUser(user: User): F[Unit]
  def saveAccount(account: Account): F[Unit]
  def findUserById(id: String): F[Option[User]]
  def findAccountByTypeAndExternalId(authType: String, externalId: String): F[Option[Account]]
  def listAccountsByUserId(userId: String): F[Seq[Account]]
}

object DataBaseService {
  case class User(id: String, createdAt: Long)
  case class Account(authType: String, externalId: String, userId: String, customData: String)
}
