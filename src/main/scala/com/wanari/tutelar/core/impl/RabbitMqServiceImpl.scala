package com.wanari.tutelar.core.impl

import akka.actor.{ActorSystem, Props}
import cats.Monad
import com.spingo.op_rabbit.{Message, RabbitControl, RabbitMarshaller}
import com.wanari.tutelar.core.RabbitMqService
import spray.json._

class RabbitMqServiceImpl[F[_]: Monad](implicit system: ActorSystem) extends RabbitMqService[F] {
  import RabbitMqServiceImpl._
  import cats.syntax.applicative._

  private lazy val rabbitControl = system.actorOf(Props[RabbitControl])

  override def init: F[Unit] = {
    rabbitControl
    ().pure
  }

  override def send[A](queue: String, msg: A)(implicit msgWriter: JsonWriter[A]): Unit = {
    rabbitControl ! Message.queue(msg, queue)
  }
}

object RabbitMqServiceImpl {
  implicit def createMarshaller[A](implicit writer: JsonWriter[A]): RabbitMarshaller[A] = new RabbitMarshaller[A] {
    val encoding = "UTF-8"

    override def marshall(value: A): Array[Byte] = value.toJson.compactPrint.getBytes(encoding)

    override protected def contentType: String = "application/json"

    override protected def contentEncoding: Option[String] = Option(encoding)
  }
}
