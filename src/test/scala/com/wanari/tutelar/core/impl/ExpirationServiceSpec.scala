package com.wanari.tutelar.core.impl

import java.util.concurrent.TimeUnit

import cats.Id
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.ExpirationService._
import com.wanari.tutelar.util.DateTimeUtil

import scala.concurrent.duration.FiniteDuration

class ExpirationServiceSpec extends TestBase {
  trait TestScope {
    val now = 1000L
    implicit val config: Map[String, ExpirationConfig] = Map(
      "provider_disabled"         -> ExpirationDisabled,
      "provider_inactivity_500ms" -> ExpirationInactivity(FiniteDuration(500, TimeUnit.MILLISECONDS)),
      "provider_lifetime_500ms"   -> ExpirationLifetime(FiniteDuration(500, TimeUnit.MILLISECONDS))
    )
    implicit val dateTimeUtilMock: DateTimeUtil[Id] = new DateTimeUtil[Id] {
      override def getCurrentTimeMillis: Id[Long] = now
    }
    lazy val service = new ExpirationServiceImpl[Id]()
  }

  val times: Seq[Long] = Seq(0, 500, 1000, 1500, 2000)

  "ExpirationService" when {
    "#isExpired" should {
      times.foreach { time =>
        s"false if provider config is missing - lastActivityAt/loginAt $time" in new TestScope {
          service.isExpired("missingProviderName", lastActivityAt = time, loginAt = time) shouldBe false
        }
      }
      times.foreach { time =>
        s"ExpirationDisabled always false - lastActivityAt/loginAt $time" in new TestScope {
          service.isExpired("missingProviderName", lastActivityAt = time, loginAt = time) shouldBe false
        }
      }
      Seq(0 -> true, 500 -> true, 1000 -> false, 1500 -> false, 2000 -> false).foreach { case (time, expected) =>
        s"ExpirationInactivity lastActivityAt $time - $expected" in new TestScope {
          service.isExpired("provider_inactivity_500ms", lastActivityAt = time, loginAt = now) shouldBe expected
        }
      }
      "Use lowercase provider name. (ExpirationInactivity lastActivityAt 0 - true)" in new TestScope {
        service.isExpired("provider_inactivity_500ms", lastActivityAt = 0, loginAt = now) shouldBe true
      }

      times.foreach { time =>
        s"ExpirationInactivity loginAt do not matter $time" in new TestScope {
          service.isExpired("provider_inactivity_500ms", lastActivityAt = now, loginAt = time) shouldBe false
        }
      }
      Seq(0 -> true, 500 -> true, 1000 -> false, 1500 -> false, 2000 -> false).foreach { case (time, expected) =>
        s"ExpirationLifetime loginAt $time - $expected" in new TestScope {
          service.isExpired("provider_lifetime_500ms", lastActivityAt = now, loginAt = time) shouldBe expected
        }
      }
      times.foreach { time =>
        s"ExpirationLifetime lastActivityAt do not matter $time" in new TestScope {
          service.isExpired("provider_lifetime_500ms", lastActivityAt = time, loginAt = now) shouldBe false
        }
      }
    }
  }
}
