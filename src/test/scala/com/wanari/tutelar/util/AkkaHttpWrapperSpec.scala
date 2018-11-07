package com.wanari.tutelar.util

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.ActorMaterializer
import com.wanari.tutelar.TestBase

class AkkaHttpWrapperSpec extends TestBase {

  "AkkaHttpWrapper" should {

    "should unmarshall" in {
      import spray.json._
      import spray.json.DefaultJsonProtocol._
      case class TestClass(a: String, b: Int, c: Boolean)
      implicit val TestClassFormatter = jsonFormat3(TestClass)

      val test = TestClass("asd", 5, false)

      withActorSystem { implicit as =>
        implicit val materializer = ActorMaterializer()

        val http = new AkkaHttpWrapper()
        val entity = await(
          http.unmarshalEntityTo[TestClass](
            HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, test.toJson.toString))
          )
        )
        entity shouldBe test
      }
    }

  }

}
