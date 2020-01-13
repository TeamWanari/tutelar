package com.wanari.tutelar.core.impl

import akka.Done
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.AmqpSink
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.util.ByteString
import cats.Applicative
import com.wanari.tutelar.core.AmqpService
import com.wanari.tutelar.core.AmqpService.{AmqpConfig, AmqpQueue, AmqpQueueConfig}

import scala.concurrent.Future

class AmqpServiceImpl[F[_]: Applicative](implicit config: AmqpConfig, actorSystem: ActorSystem) extends AmqpService[F] {
  import cats.syntax.applicative._

  protected lazy val connection: AmqpConnectionProvider = {
    AmqpCachedConnectionProvider(
      AmqpUriConnectionProvider(config.uri)
    )
  }

  override def init: F[Unit] = {
    connection
    ().pure[F]
  }

  override def createQueue(queueConfig: AmqpQueueConfig): AmqpQueue = {
    val amqpSink: Sink[ByteString, Future[Done]] = {
      AmqpSink.simple(
        convertToWriteSettings(connection, queueConfig)
      )
    }

    val queue: SourceQueueWithComplete[ByteString] = {
      Source
        .queue[ByteString](queueConfig.bufferSize, OverflowStrategy.fail)
        .toMat(amqpSink)(Keep.left)
        .run()
    }

    new AmqpQueue(queue)
  }

  private def convertToWriteSettings(connectionProvider: AmqpConnectionProvider, queueConfig: AmqpQueueConfig) = {
    AmqpWriteSettings(connectionProvider)
      .withRoutingKey(queueConfig.queueName)
      .withDeclaration(QueueDeclaration(queueConfig.queueName))
  }
}
