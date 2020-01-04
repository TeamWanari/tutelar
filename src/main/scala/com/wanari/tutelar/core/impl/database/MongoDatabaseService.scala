package com.wanari.tutelar.core.impl.database

import cats.data.{EitherT, OptionT}
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.DatabaseService.{Account, AccountId, User, UserIdWithExternalId}
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.monocle.field
import reactivemongo.api.{AsyncDriver, Cursor, MongoConnection}

import scala.concurrent.{ExecutionContext, Future}

class MongoDatabaseService(implicit config: MongoConfig, ec: ExecutionContext, driver: AsyncDriver)
    extends DatabaseService[Future] {
  import MongoDatabaseService._
  import cats.instances.future._

  private lazy val usersCollection: Future[BSONCollection] = {
    val result: EitherT[Future, Throwable, BSONCollection] = for {
      uri        <- EitherT(MongoConnection.fromString(config.uri).map(Right(_)).recover(Left(_)))
      dbname     <- EitherT.fromOption(uri.db, WrongConfig("Database name not found!"))
      connection <- EitherT(driver.connect(uri).map(Right(_)).recover(Left(_)))
      db         <- EitherT.right(connection.database(dbname))
    } yield db.collection[BSONCollection](config.collection)

    result.foldF(Future.failed, Future.successful)
  }

  override def init: Future[Unit] = {
    usersCollection.map(_ => ())
  }

  override def checkStatus(): Future[Boolean] = {
    usersCollection.map(_ => true).recover { case _ => false }
  }

  override def saveUser(user: User): Future[Unit] = {
    val data = UserDbModel(user.id, user.createdAt, Seq.empty)
    runUnit(_.insert.one(data))
  }

  override def saveAccount(account: Account): Future[Unit] = {
    val selector = userSelector(account.userId)
    val data     = AccountDbModel(account.authType, account.externalId, account.customData)
    val modifier = document(
      "$push" -> document("accounts" -> accountHandler.writeOpt(data).getOrElse(BSONDocument.empty))
    )
    runUnit(_.update.one(selector, modifier))
  }

  override def findUserById(id: String): Future[Option[User]] = {
    val selector = userSelector(id)
    runOptionT(_.find(selector, fullProjector).one[UserDbModel])
      .map(_.toUser)
      .value
  }

  override def findAccountByTypeAndExternalId(accountId: AccountId): Future[Option[Account]] = {
    val selector = accountSelector(accountId)
    runOptionT(_.find(selector, fullProjector).one[UserDbModel]).subflatMap { user =>
      user.accounts
        .find(_.authType == accountId._1)
        .map(_.toAccount(user.id))
    }.value
  }

  override def listAccountsByUserId(userId: String): Future[Seq[Account]] = {
    val selector = userSelector(userId)
    runOptionT(_.find(selector, fullProjector).one[UserDbModel])
      .map(user => user.accounts.map(_.toAccount(user.id)))
      .getOrElse(Seq.empty)
  }

  override def updateCustomData(accountId: AccountId, customData: String): Future[Unit] = {
    val selector = accountSelector(accountId)
    val modifier = document("$set" -> document("accounts.$.customData" -> customData))
    runUnit(_.update.one(selector, modifier))
  }

  override def deleteUserWithAccountsById(userId: String): Future[Unit] = {
    val selector = userSelector(userId)
    runUnit(_.delete().one(selector))
  }

  override def deleteAccountByUserAndType(userId: String, authType: String): Future[Unit] = {
    val selector = userSelector(userId)
    val modifier = document("$pull" -> document("accounts" -> document("authType" -> authType)))
    runUnit(_.update.one(selector, modifier))
  }

  override def listUserIdsByAuthType(authType: String): Future[Seq[UserIdWithExternalId]] = {
    implicit val reader = readerUserIdWithExternalId(authType)

    val selector  = document("accounts" -> document("$elemMatch" -> document("authType" -> authType)))
    val projector = Option(document("id" -> 1, "accounts.externalId" -> 1, "accounts.authType" -> 1))
    usersCollection.flatMap(
      _.find(selector, projector)
        .cursor[UserIdWithExternalId]()
        .collect[List](-1, Cursor.FailOnError[List[UserIdWithExternalId]]())
    )
  }

  private def runUnit(f: BSONCollection => Future[Any]) = {
    usersCollection.flatMap(f).map(_ => ())
  }

  private def runOptionT[A](f: BSONCollection => Future[Option[A]]) = {
    OptionT(usersCollection.flatMap(f))
  }

  private def accountSelector(accountId: AccountId) = {
    document("accounts" -> document("$elemMatch" -> document("authType" -> accountId._1, "externalId" -> accountId._2)))
  }

  private def userSelector(userId: String) = {
    document("id" -> userId)
  }

  private val fullProjector = {
    Option(document("id" -> 1, "createdAt" -> 1, "accounts" -> 1))
  }

  private def readerUserIdWithExternalId(authType: String): BSONDocumentReader[UserIdWithExternalId] =
    BSONDocumentReader { doc =>
      val lensId         = field[String]("id")
      val lensAccounts   = field[BSONArray]("accounts")
      val lensAuthType   = field[String]("authType")
      val lensExternalId = field[String]("externalId")

      val result = for {
        id <- lensId.getOption(doc)
        externalId <- lensAccounts.getOption(doc).flatMap { accounts: BSONArray =>
          accounts.values
            .collect { case acc: BSONDocument => (lensAuthType.getOption(acc), lensExternalId.getOption(acc)) }
            .collectFirst { case (Some(auth), Some(externalId)) if auth == authType => externalId }
        }
      } yield UserIdWithExternalId(id, externalId)

      result.getOrElse(throw new Exception("Wrong schema"))
    }
}

object MongoDatabaseService {
  private case class UserDbModel(id: String, createdAt: Long, accounts: Seq[AccountDbModel]) {
    def toUser = User(id, createdAt)
  }
  private case class AccountDbModel(authType: String, externalId: String, customData: String) {
    def toAccount(userId: String) = Account(authType, externalId, userId, customData)
  }
  private implicit def accountHandler: BSONDocumentHandler[AccountDbModel] = Macros.handler[AccountDbModel]
  private implicit def userHandler: BSONDocumentHandler[UserDbModel]       = Macros.handler[UserDbModel]

  case class MongoConfig(uri: String, collection: String)
}
