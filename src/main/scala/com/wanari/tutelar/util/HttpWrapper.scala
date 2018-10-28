package com.wanari.tutelar.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import spray.json.RootJsonReader

import scala.concurrent.Future
import scala.reflect.ClassTag

trait HttpWrapper[F[_]] {
  def singleRequest(httpRequest: HttpRequest): F[HttpResponse]
  def unmarshalEntityTo[T: ClassTag: RootJsonReader](resp: HttpResponse): F[T]
}

class AkkaHttpWrapper(implicit actorSystem: ActorSystem, materializer: Materializer) extends HttpWrapper[Future] {

  override def singleRequest(httpRequest: HttpRequest): Future[HttpResponse] = {
    Http().singleRequest(httpRequest)
  }

  override def unmarshalEntityTo[T: ClassTag: RootJsonReader](resp: HttpResponse): Future[T] = {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    Unmarshal(resp.entity).to[T]
  }
}
