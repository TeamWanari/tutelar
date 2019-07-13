package com.wanari.tutelar.core.impl.database

import cats.data.{EitherT, OptionT}
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.DatabaseService.{Account, AccountId, User}
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDocument, BSONDocumentHandler, BSONInteger, Macros}

import scala.concurrent.{ExecutionContext, Future}

class MongoDatabaseService(config: => MongoConfig)(implicit ec: ExecutionContext, driver: MongoDriver)
    extends DatabaseService[Future] {
  import MongoDatabaseService._
  import cats.instances.future._

  private lazy val usersCollection: Future[BSONCollection] = {
    val result: EitherT[Future, Throwable, BSONCollection] = for {
      uri        <- EitherT.fromEither(MongoConnection.parseURI(config.uri).toEither)
      dbname     <- EitherT.fromOption(uri.db, WrongConfig("Database name not found!"))
      connection <- EitherT.fromEither(driver.connection(config.uri).toEither)
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
    val modifier = BSONDocument("$push" -> BSONDocument("accounts" -> accountHandler.write(data)))
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
    val modifier = BSONDocument("$set" -> BSONDocument("accounts.$.customData" -> customData))
    runUnit(_.update.one(selector, modifier))
  }

  override def deleteUserWithAccountsById(userId: String): Future[Unit] = {
    val selector = userSelector(userId)
    runUnit(_.delete().one(selector))
  }

  override def deleteAccountByUserAndType(userId: String, authType: String): Future[Unit] = {
    val selector = userSelector(userId)
    val modifier = BSONDocument("$pull" -> BSONDocument("accounts" -> BSONDocument("authType" -> authType)))
    runUnit(_.update.one(selector, modifier))
  }

  private def runUnit(f: BSONCollection => Future[Any]) = {
    usersCollection.flatMap(f).map(_ => ())
  }

  private def runOptionT[A](f: BSONCollection => Future[Option[A]]) = {
    OptionT(usersCollection.flatMap(f))
  }

  private def accountSelector(accountId: AccountId) = {
    BSONDocument(
      "accounts" -> BSONDocument("$elemMatch" -> BSONDocument("authType" -> accountId._1, "externalId" -> accountId._2))
    )
  }

  private def userSelector(userId: String) = {
    BSONDocument("id" -> userId)
  }

  private val fullProjector = {
    val fields = Seq("id", "createdAt", "accounts")
    Option(BSONDocument(fields.map(_ -> BSONInteger(1))))
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
