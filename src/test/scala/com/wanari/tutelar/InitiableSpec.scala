package com.wanari.tutelar

import com.wanari.tutelar.core.config.{RuntimeConfig, ServerConfig}
import org.slf4j.LoggerFactory

import scala.util.{Success, Try}

class InitiableSpec extends TestBase {

  trait TestScope {
    var called = false
    var loaded = false
    lazy val service = new Initable[Try] {
      loaded = true
      override def init: Try[Unit] = {
        called = true
        Success(())
      }
    }
    implicit val logger = LoggerFactory.getLogger("test")
    implicit val config = new ServerConfig[Try] {
      override def getVersion: Try[String]             = ???
      override def getHostname: Try[String]            = ???
      override def getEnabledModules: Try[Seq[String]] = Success(Seq("modulename"))
      override val runtimeConfig: RuntimeConfig[Try]   = null
      override def init: Try[Unit]                     = ???
    }
  }
  import cats.instances.try_._

  "Initiable" should {
    "#initializeIfEnabled" should {
      "call init if enabled" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename") shouldEqual Success(())
        called shouldBe true
      }
      "do not call init if disabled" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename2") shouldEqual Success(())
        called shouldBe false
      }
      "do not instantiating the service" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename2") shouldEqual Success(())
        loaded shouldBe false
      }
    }
  }

}
