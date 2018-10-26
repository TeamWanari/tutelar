package com.wanari.tutelar

import com.wanari.tutelar.HealthCheckService.HealthCheckResult
import org.mockito.Mockito.when

import scala.util.{Success, Failure, Try}

class HealthCheckServiceSpec extends TestBase {

  trait TestScope {
    import cats.instances.try_._
    implicit val configService: ConfigService[Try]         = mock[ConfigService[Try]]
    implicit val databaseServiceMock: DatabaseService[Try] = mock[DatabaseService[Try]]
    val service                                            = new HealthCheckServiceImpl[Try]()
  }

  "#getStatus" when {
    "ok" in new TestScope {
      when(configService.getVersion).thenReturn(Success("TestVersionMock"))
      when(configService.getHostname).thenReturn(Success("TestHostnameMock"))
      when(databaseServiceMock.checkStatus()).thenReturn(Success(true))

      service.getStatus.get shouldEqual HealthCheckResult(true, "TestVersionMock", "TestHostnameMock", true)
    }
    "version failed" in new TestScope {
      when(configService.getVersion).thenReturn(Failure(new Exception))
      when(configService.getHostname).thenReturn(Success("TestHostnameMock"))
      when(databaseServiceMock.checkStatus()).thenReturn(Success(true))

      service.getStatus.get shouldEqual HealthCheckResult(false, "", "TestHostnameMock", true)
    }
    "hostname failed" in new TestScope {
      when(configService.getVersion).thenReturn(Success("TestVersionMock"))
      when(configService.getHostname).thenReturn(Failure(new Exception))
      when(databaseServiceMock.checkStatus()).thenReturn(Success(true))

      service.getStatus.get shouldEqual HealthCheckResult(false, "TestVersionMock", "", true)
    }
    "db failed" in new TestScope {
      when(configService.getVersion).thenReturn(Success("TestVersionMock"))
      when(configService.getHostname).thenReturn(Success("TestHostnameMock"))
      when(databaseServiceMock.checkStatus()).thenReturn(Success(false))

      service.getStatus.get shouldEqual HealthCheckResult(false, "TestVersionMock", "TestHostnameMock", false)
    }
    "db check failed" in new TestScope {
      when(configService.getVersion).thenReturn(Success("TestVersionMock"))
      when(configService.getHostname).thenReturn(Success("TestHostnameMock"))
      when(databaseServiceMock.checkStatus()).thenReturn(Failure(new Exception))

      service.getStatus.get shouldEqual HealthCheckResult(false, "TestVersionMock", "TestHostnameMock", false)
    }
  }
}
