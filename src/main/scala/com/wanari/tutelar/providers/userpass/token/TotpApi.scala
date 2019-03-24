package com.wanari.tutelar.providers.userpass.token

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, entity, path, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.providers.userpass.UserPassApi
import com.wanari.tutelar.providers.userpass.token.TotpApi.RegisterData
import spray.json.{DefaultJsonProtocol, JsObject, RootJsonFormat}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class TotpApi(
    implicit val service: TotpService[Future],
    val callbackConfig: () => Future[CallbackConfig]
) extends UserPassApi {
  override val servicePath: String = "totp"

  override def route(): Route = {
    super.route() ~
      pathPrefix(servicePath) {
        path("register") {
          post {
            entity(as[RegisterData]) { data =>
              completeLoginFlowWithJson(service.register(data.username, data.token, data.password, data.data))
            }
          }
        } ~
          path("qrCode") {
            get {
              import TotpServiceImpl._
              import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

              onComplete(service.qrCodeData) {
                case Success(t) => complete(t)
                case Failure(_) => complete(StatusCodes.InternalServerError)
              }
            }
          }
      }
  }
}

object TotpApi {
  case class RegisterData(username: String, token: String, password: String, data: Option[JsObject])

  import DefaultJsonProtocol._
  implicit val registerDataFormat: RootJsonFormat[RegisterData] = jsonFormat4(RegisterData)
}
