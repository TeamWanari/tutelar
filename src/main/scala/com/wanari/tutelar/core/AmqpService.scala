package com.wanari.tutelar.core

import akka.stream.scaladsl.SourceQueueWithComplete
import akka.util.ByteString
import com.typesafe.config.Config
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.AmqpService.{AmqpQueue, AmqpQueueConfig}

trait AmqpService[F[_]] extends Initable[F] {
  def createQueue(queueConfig: AmqpQueueConfig): AmqpQueue
}

object AmqpService {
  case class AmqpConfig(uri: String)

  case class AmqpQueueConfig(queueName: String, bufferSize: Int)

  object AmqpQueueConfig {
    def apply(config: Config): AmqpQueueConfig = {
      AmqpQueueConfig(
        config.getString("queue-name"),
        config.getInt("buffer-size")
      )
    }
  }

  class AmqpQueue(queue: SourceQueueWithComplete[ByteString]) {
    import spray.json._
    def send[A](msg: A)(implicit msgWriter: JsonWriter[A]): Unit = {
      val message = ByteString(msg.toJson.compactPrint.getBytes("UTF8"))
      queue.offer(message)
    }
  }
}
