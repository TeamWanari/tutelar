package com.wanari.tutelar.core

import com.wanari.tutelar.Initable
import spray.json.JsonWriter

trait RabbitMqService[F[_]] extends Initable[F] {
  def send[A](queue: String, msg: A)(implicit msgWriter: JsonWriter[A]): Unit
}
