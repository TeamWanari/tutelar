package com.wanari.tutelar.providers.userpass.email

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.providers.userpass.UserPassApi
import com.wanari.tutelar.providers.userpass.email.EmailProviderApi._
import spray.json.{DefaultJsonProtocol, JsObject, RootJsonFormat}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future
import scala.util.Success

class EmailProviderApi(
    implicit val service: EmailProviderService[Future],
    val callbackConfig: () => Future[CallbackConfig]
) extends UserPassApi {
  override val servicePath: String = "email"

  override def route(): Route = {
    super.route() ~
      pathPrefix(servicePath) {
        path("register") {
          post {
            entity(as[RegisterData]) { data =>
              completeLoginFlowWithJson(service.register(data.token, data.password, data.data))
            }
          }
        } ~ path("send-register") {
          post {
            entity(as[EmailData]) { data =>
              onComplete(service.sendRegister(data.email)) {
                case Success(_) => complete(StatusCodes.OK)
                case _          => complete(StatusCodes.InternalServerError)
              }
            }
          }
        }
      }
  }
}

object EmailProviderApi {
  case class EmailData(email: String)
  case class RegisterData(token: String, password: String, data: Option[JsObject])
  import DefaultJsonProtocol._
  implicit val registerDataFormat: RootJsonFormat[RegisterData] = jsonFormat3(RegisterData)
  implicit val emailDataFormat: RootJsonFormat[EmailData]       = jsonFormat1(EmailData)
}
