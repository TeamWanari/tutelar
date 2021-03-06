package com.wanari.tutelar.core.database

import cats.data.OptionT
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.AwaitUtil
import com.wanari.tutelar.core.DatabaseService.{Account, User, UserIdWithExternalId}
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.database.PostgresDatabaseService.PostgresConfig
import com.wanari.tutelar.core.impl.database.{MemoryDatabaseService, MongoDatabaseService, PostgresDatabaseService}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{MongoConnection, AsyncDriver}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class DatabaseServiceItSpec extends AnyWordSpecLike with Matchers with AwaitUtil with BeforeAndAfterAll {
  import cats.instances.future._
  import slick.jdbc.PostgresProfile.api.Database

  override implicit val timeout = 15.seconds

  private val memoryService = new MemoryDatabaseService[Future]

  private val config = ConfigFactory.load()

  private val db                      = Database.forConfig("database.postgres")
  private implicit val postgresConfig = PostgresConfig(config, "database.postgres")
  private val postgresService         = new PostgresDatabaseService

  private implicit val mongoDriver = new AsyncDriver()
  private implicit val mongoConfig = MongoConfig(
    config.getString("database.mongo.uri"),
    config.getString("database.mongo.collection")
  )
  private val mongoService = new MongoDatabaseService
  private val mongoCollection = await({
    (for {
      uri        <- OptionT(MongoConnection.fromString(mongoConfig.uri).map(Option(_)).recover(_ => None))
      dbname     <- OptionT.fromOption(uri.db)
      connection <- OptionT(mongoDriver.connect(uri).map(Option(_)).recover(_ => None))
      database   <- OptionT.liftF(connection.database(dbname))
    } yield database.collection[BSONCollection](mongoConfig.collection)).getOrElseF(Future.failed(new Exception("")))
  })

  override def beforeAll(): Unit = truncateDb()

  override def afterAll(): Unit = {
    truncateDb()
    mongoDriver.close(3.seconds)
  }

  private def truncateDb(): Unit = {
    import slick.jdbc.PostgresProfile.api._
    await(for {
      _ <- db.run(sqlu"TRUNCATE USERS CASCADE;")
      _ <- mongoCollection.delete().one(BSONDocument())
    } yield ())
  }

  Seq(
    "postgres slick instance" -> postgresService,
    "memory instance"         -> memoryService,
    "mongodb instance"        -> mongoService
  ).foreach { case (name, service) =>
    name when {
      "CheckStatus" in {
        await(service.checkStatus()) shouldEqual true
      }

      "Users" should {
        "save and find" in {
          val user1 = User("id1", 1)
          val user2 = User("id2", 2)

          await(service.findUserById(user1.id)) shouldEqual None
          await(service.findUserById(user2.id)) shouldEqual None

          await(service.saveUser(user1))
          await(service.saveUser(user2))

          await(service.findUserById(user1.id)) shouldEqual Some(user1)
          await(service.findUserById(user2.id)) shouldEqual Some(user2)
        }
      }

      "Accounts" should {
        "save and find by external id and auth type" in {
          val user = User("user1", 1)
          await(service.saveUser(user))

          val account1 = Account("type1", "ext1", user.id, "XXX1")
          val account2 = Account("type2", "ext1", user.id, "XXX2")

          await(service.findAccountByTypeAndExternalId(account1.getId)) shouldEqual None
          await(service.findAccountByTypeAndExternalId(account2.getId)) shouldEqual None

          await(service.saveAccount(account1))
          await(service.saveAccount(account2))

          await(service.findAccountByTypeAndExternalId(account1.getId)) shouldEqual Some(account1)
          await(service.findAccountByTypeAndExternalId(account2.getId)) shouldEqual Some(account2)
        }

        "save and list by user" in {
          val user1 = User("user2", 1)
          val user2 = User("user3", 2)

          await(service.saveUser(user1))
          await(service.saveUser(user2))

          await(service.listAccountsByUserId(user1.id)) shouldEqual Seq()
          await(service.listAccountsByUserId(user2.id)) shouldEqual Seq()

          val account1 = Account("type3", "ext1", user1.id, "XXX4")
          val account2 = Account("type4", "ext1", user1.id, "XXX5")
          val account3 = Account("type3", "ext2", user2.id, "XXX6")

          await(service.saveAccount(account1))
          await(service.saveAccount(account2))
          await(service.saveAccount(account3))

          await(service.listAccountsByUserId(user1.id)) shouldEqual Seq(account1, account2)
          await(service.listAccountsByUserId(user2.id)) shouldEqual Seq(account3)
        }

        "updateCustomData" in {
          val user     = User("user4", 1)
          val account1 = Account("type5", "ext1", user.id, "XXX")
          val account2 = Account("type6", "ext1", user.id, "XXX")

          await(service.saveUser(user))
          await(service.saveAccount(account1))
          await(service.saveAccount(account2))

          await(service.updateCustomData(account1.getId, "ZZZ"))

          await(service.findAccountByTypeAndExternalId(account1.getId)) shouldEqual Some(
            account1.copy(customData = "ZZZ")
          )
          await(service.findAccountByTypeAndExternalId(account2.getId)) shouldEqual Some(
            account2
          )
        }

        "deleteUserWithAccountsById" in {
          val user1    = User("id3", 1)
          val user2    = User("id4", 2)
          val account1 = Account("type6", "ext2", user1.id, "XXX4")
          val account2 = Account("type7", "ext1", user1.id, "XXX5")
          val account3 = Account("type6", "ext3", user2.id, "XXX6")

          await(service.saveUser(user1))
          await(service.saveUser(user2))
          await(service.saveAccount(account1))
          await(service.saveAccount(account2))
          await(service.saveAccount(account3))

          await(service.deleteUserWithAccountsById(user1.id))

          await(service.listAccountsByUserId(user1.id)) shouldEqual Seq.empty
          await(service.listAccountsByUserId(user2.id)) shouldEqual Seq(account3)

          await(service.findUserById(user1.id)) shouldEqual None
          await(service.findUserById(user2.id)) shouldEqual Some(user2)
        }

        "deleteAccountByUserAndType" in {
          val user1    = User("id5", 1)
          val user2    = User("id6", 2)
          val account1 = Account("type10", "ext1", user1.id, "XXX4")
          val account2 = Account("type11", "ext1", user1.id, "XXX5")
          val account3 = Account("type11", "ext2", user2.id, "XXX6")

          await(service.saveUser(user1))
          await(service.saveUser(user2))
          await(service.saveAccount(account1))
          await(service.saveAccount(account2))
          await(service.saveAccount(account3))

          await(service.deleteAccountByUserAndType(user1.id, account2.authType))

          await(service.listAccountsByUserId(user1.id)) shouldEqual Seq(account1)
          await(service.listAccountsByUserId(user2.id)) shouldEqual Seq(account3)
        }
        "listUserIdsByAuthType" in {
          val user1    = User("id7", 1)
          val user2    = User("id8", 2)
          val user3    = User("id9", 3)
          val account1 = Account("type20", "ext1", user1.id, "XXX4")
          val account2 = Account("type21", "ext1", user1.id, "XXX5")
          val account3 = Account("type21", "ext2", user2.id, "XXX6")
          val account4 = Account("type21", "ext3", user3.id, "XXX7")

          await(service.saveUser(user1))
          await(service.saveUser(user2))
          await(service.saveUser(user3))
          await(service.saveAccount(account1))
          await(service.saveAccount(account2))
          await(service.saveAccount(account3))
          await(service.saveAccount(account4))

          await(service.listUserIdsByAuthType("type20")).toSet shouldEqual Set(
            UserIdWithExternalId(user1.id, account1.externalId)
          )
          await(service.listUserIdsByAuthType("type21")).toSet shouldEqual Set(
            UserIdWithExternalId(user1.id, account2.externalId),
            UserIdWithExternalId(user2.id, account3.externalId),
            UserIdWithExternalId(user3.id, account4.externalId)
          )
          await(service.listUserIdsByAuthType("typeXX")).toSet shouldEqual Set()
        }
      }
    }
  }
}
