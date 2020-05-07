package com.wanari.tutelar.core.impl.database

import cats.Applicative
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.{ConfigService, DatabaseService}
import reactivemongo.api.AsyncDriver

import scala.concurrent.{ExecutionContext, Future}

object DatabaseServiceFactory {
  def create()(implicit
      config: ConfigService,
      driver: AsyncDriver,
      ev: Applicative[Future],
      ec: ExecutionContext
  ): DatabaseService[Future] = {
    import config._
    config.getDatabaseConfig.`type` match {
      case DatabaseConfig.MEMORY   => new MemoryDatabaseService[Future]
      case DatabaseConfig.POSTGRES => new PostgresDatabaseService
      case DatabaseConfig.MONGO    => new MongoDatabaseService
      case _                       => throw WrongConfig(s"Unsupported database type: ${config.getDatabaseConfig.`type`}")
    }
  }

  case class DatabaseConfig(`type`: String)
  object DatabaseConfig {
    val MEMORY   = "memory"
    val POSTGRES = "postgres"
    val MONGO    = "mongo"
  }
}
