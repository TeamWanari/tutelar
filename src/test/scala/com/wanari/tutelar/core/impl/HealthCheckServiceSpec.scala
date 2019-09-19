package com.wanari.tutelar.core.impl

import cats.Id
import com.wanari.tutelar.core.HealthCheckService.HealthCheckResult
import com.wanari.tutelar.core.{ConfigService, DatabaseService}
import com.wanari.tutelar.{BuildInfo, TestBase}
import org.mockito.Mockito.when

class HealthCheckServiceSpec extends TestBase {

  trait TestScope {
    implicit val configService: ConfigService             = mock[ConfigService]
    implicit val databaseServiceMock: DatabaseService[Id] = mock[DatabaseService[Id]]
    val service                                           = new HealthCheckServiceImpl[Id]()
  }

  "#getStatus" when {
    "ok" in new TestScope {
      when(databaseServiceMock.checkStatus()).thenReturn(true)

      service.getStatus.value shouldEqual Right(
        HealthCheckResult(
          true,
          BuildInfo.version,
          true,
          BuildInfo.builtAtString,
          BuildInfo.builtAtMillis,
          BuildInfo.commitHash
        )
      )
    }
    "db failed" in new TestScope {
      when(databaseServiceMock.checkStatus()).thenReturn(false)

      service.getStatus.value shouldEqual Right(
        HealthCheckResult(
          false,
          BuildInfo.version,
          false,
          BuildInfo.builtAtString,
          BuildInfo.builtAtMillis,
          BuildInfo.commitHash
        )
      )
    }
  }
}
