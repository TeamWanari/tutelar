package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api
import com.wanari.tutelar.core.CoreApi._
import spray.json._

import scala.concurrent.Future

class CoreApi(implicit val authService: AuthService[Future]) extends Api with AuthDirectives {
  override def route(): Route = {
    pathPrefix("core") {
      userAuth { userId =>
        path("delete") {
          post {
            withTrace("Delete_core") { _ =>
              onSuccess(authService.deleteUser(userId)) {
                complete(StatusCodes.OK)
              }
            }
          }
        } ~
          path("unlink") {
            post {
              entity(as[UnlinkData]) { data =>
                withTrace("Unlink_core") { _ =>
                  onSuccess(authService.unlink(userId, data.authType)) {
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
          }
      }
    }
  }
}

object CoreApi {
  case class UnlinkData(authType: String)
  import spray.json.DefaultJsonProtocol._
  implicit val formatUnlinkData: RootJsonFormat[UnlinkData] = jsonFormat1(UnlinkData)
}
