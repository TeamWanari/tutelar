package com.wanari.tutelar.core.impl.database

import cats.Applicative
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.config.ServerConfig
import reactivemongo.api.MongoDriver

import scala.concurrent.{ExecutionContext, Future}

object DatabaseServiceFactory {
  def create()(
      implicit config: ServerConfig[Future],
      driver: MongoDriver,
      ev: Applicative[Future],
      ec: ExecutionContext
  ): DatabaseService[Future] = {
    config.getDatabaseConfig.`type` match {
      case DatabaseConfig.MEMORY   => new MemoryDatabaseService[Future]
      case DatabaseConfig.POSTGRES => new PostgresDatabaseService(PostgresDatabaseService.getDatabase)
      case DatabaseConfig.MONGO    => new MongoDatabaseService(config.getMongoConfig)
    }
  }

  case class DatabaseConfig(`type`: String)
  object DatabaseConfig {
    val MEMORY   = "memory"
    val POSTGRES = "postgres"
    val MONGO    = "mongo"
  }
}
