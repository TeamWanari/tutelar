package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api
import com.wanari.tutelar.core.AuthService.LongTermToken
import com.wanari.tutelar.core.CoreApi._
import com.wanari.tutelar.core.ProviderApi._
import spray.json._

import scala.concurrent.Future

class CoreApi(implicit val authService: AuthService[Future]) extends Api with AuthDirectives {
  override def route(): Route = {
    pathPrefix("core") {
      userAuth { userId =>
        path("delete") {
          post {
            withTrace("Delete_core") { implicit ctx =>
              onSuccess(authService.deleteUser(userId)) {
                complete(StatusCodes.OK)
              }
            }
          }
        } ~
          path("unlink") {
            post {
              entity(as[UnlinkData]) { data =>
                withTrace("Unlink_core") { implicit ctx =>
                  onSuccess(authService.unlink(userId, data.authType)) {
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
          }
      } ~
        path("refresh-token") {
          post {
            entity(as[RefreshTokenData]) { data =>
              withTrace("RefreshToken_core") { implicit ctx =>
                onSuccess(authService.refreshToken(data.refreshToken).value) {
                  case Some(token) => complete(token)
                  case _           => complete((StatusCodes.Unauthorized, ErrorData(ProviderApi.Errors.AUTHENTICATION_FAILED)))
                }
              }
            }
          }
        }
    }
  }
}

object CoreApi {
  case class RefreshTokenData(refreshToken: LongTermToken)
  case class UnlinkData(authType: String)
  import spray.json.DefaultJsonProtocol._
  implicit val formatRefreshTokenData: RootJsonFormat[RefreshTokenData] = jsonFormat1(RefreshTokenData)
  implicit val formatUnlinkData: RootJsonFormat[UnlinkData]             = jsonFormat1(UnlinkData)
}
