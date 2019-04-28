package com.wanari.tutelar.core
import cats.Monad
import spray.json._

class RabbitMqServiceMock[F[_]: Monad] extends RabbitMqService[F] {
  import cats.syntax.applicative._

  var calls = Seq.empty[(String, JsValue)]

  override def send[A](queue: String, msg: A)(implicit msgWriter: JsonWriter[A]): Unit = calls :+= (queue -> msg.toJson)

  override def init: F[Unit] = ().pure
}
