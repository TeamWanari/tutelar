package com.wanari.tutelar.providers.userpass.token

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{as, entity, path, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.AuthService.LongTermToken
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.providers.userpass.UserPassApi
import com.wanari.tutelar.providers.userpass.token.TotpApi.RegisterData
import spray.json.{DefaultJsonProtocol, JsObject, RootJsonFormat}

import scala.concurrent.Future

class TotpApi(implicit
    val service: TotpService[Future],
    val callbackConfig: CallbackConfig
) extends UserPassApi {
  override val servicePath: String = "totp"

  override def route(): Route = {
    super.route() ~
      pathPrefix(servicePath) {
        path("register") {
          post {
            entity(as[RegisterData]) { data =>
              withTrace(s"Register_$servicePath") { implicit ctx =>
                completeLoginFlowWithJson(
                  service.register(data.username, data.token, data.password, data.data, data.refreshToken)
                )
              }
            }
          }
        } ~
          path("qr-code") {
            get {
              withTrace(s"QrCode_$servicePath") { implicit ctx =>
                import TotpServiceImpl._
                service.qrCodeData.toComplete
              }
            }
          }
      }
  }
}

object TotpApi {
  case class RegisterData(
      username: String,
      token: String,
      password: String,
      data: Option[JsObject],
      refreshToken: Option[LongTermToken]
  )

  import DefaultJsonProtocol._
  implicit val registerDataFormat: RootJsonFormat[RegisterData] = jsonFormat5(RegisterData)
}
