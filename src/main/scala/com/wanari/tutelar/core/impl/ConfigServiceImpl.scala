package com.wanari.tutelar.core.impl

import java.io.{BufferedWriter, File, FileWriter}
import java.util.concurrent.TimeUnit

import com.emarsys.escher.akka.http.config.EscherConfig
import com.typesafe.config.{Config, ConfigFactory}
import com.wanari.tutelar.core.AmqpService.{AmqpConfig, AmqpQueueConfig}
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.core.ExpirationService._
import com.wanari.tutelar.core.HookService.HookConfig
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.ServiceAuthDirectives.{AccessBlocked, JwtAuthConfig}
import com.wanari.tutelar.core.TracerService.{JaegerConfig, TracerServiceConfig}
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.database.PostgresDatabaseService.PostgresConfig
import com.wanari.tutelar.core.{ConfigService, HookService, ServiceAuthDirectives}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceSmtpImpl.{
  EmailServiceSmtpConfig,
  EmailTemplateConfig,
  SmtpConfig
}
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.OTP.OTPAlgorithm
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig
import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.io.Source
import scala.util.{Try, Using}

class ConfigServiceImpl() extends ConfigService {
  private lazy val logger = LoggerFactory.getLogger("CONFIG")

  private lazy val conf: Config = {
    Try {
      val applicationConfig = listConfigFilesIfDirectoryExists().fold(ConfigFactory.load()) { configFiles =>
        if (configFiles.isEmpty) {
          copyResourceConfigToDirectory()
          ConfigFactory.load()
        } else {
          mergeConfigFiles(configFiles)
        }
      }
      val extraConfig = ConfigFactory.load("extra.conf")
      applicationConfig.withFallback(extraConfig)
    }
  }.fold(logAndThrow(""), identity)

  private def mergeConfigFiles(files: Seq[File]): Config = {
    files
      .sortBy(_.getName)
      .map(ConfigFactory.parseFile)
      .fold(ConfigFactory.empty())(_ withFallback _)
      .resolve()
  }

  private def listConfigFilesIfDirectoryExists(): Option[Seq[File]] = {
    val configDir = new File("/config")
    if (configDir.exists && configDir.isDirectory) {
      Some(
        configDir.listFiles.toSeq
          .filter(_.isFile)
          .filter(_.getName.endsWith(".conf"))
          .sortBy(_.getName)
      )
    } else {
      None
    }
  }

  private def copyResourceConfigToDirectory(): Unit = {
    Using.Manager { use =>
      val in  = use(Source.fromResource("application.conf"))
      val out = use(new BufferedWriter(new FileWriter("/config/application.conf")))
      in.iter.foreach { c =>
        out.write(c)
      }
    }
  }

  override lazy val getEnabledModules: Seq[String] = {
    Try(getListConfig(conf, "modulesEnabled")).fold(logAndThrow("Enabled modules"), identity)
  }

  override implicit lazy val getPostgresConfig: PostgresConfig = {
    Try {
      val config = conf.getConfig("database.postgres")
      val url    = readFromFileOrConf(config, "url").replaceAll("\n", "")
      val databaseConfig = config
        .withoutPath("url")
        .withoutPath("urlFile")
        .withFallback(ConfigFactory.parseString(s"""url = "$url""""))
      PostgresConfig(databaseConfig, "")
    }.fold(logAndThrow("PostgreSQL"), identity)
  }

  override implicit lazy val getMongoConfig: MongoConfig = {
    Try {
      val config = conf.getConfig("database.mongo")
      MongoConfig(
        readFromFileOrConf(config, "uri"),
        config.getString("collection")
      )
    }.fold(logAndThrow("Mongo"), identity)
  }

  override implicit lazy val getDatabaseConfig: DatabaseConfig = {
    Try {
      val config = conf.getConfig("database")
      DatabaseConfig(
        config.getString("type")
      )
    }.fold(logAndThrow("Database type selector"), identity)
  }

  override implicit lazy val getTracerServiceConfig: TracerServiceConfig = {
    Try {
      val config = conf.getConfig("tracer")
      TracerServiceConfig(
        config.getString("client")
      )
    }.fold(logAndThrow("Tracer"), identity)
  }

  override implicit def getJwtConfigByName(name: String): JwtConfig = {
    Try {
      val config = conf.getConfig(s"jwt.$name")
      JwtConfig(
        getDuration(config, "expirationTime"),
        config.getString("algorithm"),
        Try(readFromFileOrConf(config, "secret")).getOrElse(""),
        Try(readFromFileOrConf(config, "privateKey")).getOrElse(""),
        Try(readFromFileOrConf(config, "publicKey")).getOrElse("")
      )
    }.fold(logAndThrow(s"JWT for $name"), identity)
  }

  override implicit lazy val getCallbackConfig: CallbackConfig = {
    Try {
      val config = conf.getConfig("callback")
      CallbackConfig(
        config.getString("success"),
        config.getString("failure")
      )
    }.fold(logAndThrow("Callback"), identity)
  }

  override implicit lazy val getHookConfig: HookConfig = {
    Try {
      val config = conf.getConfig("hook")
      val authConfig = config.getString("authType") match {
        case "basic" =>
          HookService.BasicAuthConfig(
            config.getString("basicAuth.username"),
            readFromFileOrConf(config, "basicAuth.password")
          )
        case "escher" => HookService.EscherAuthConfig
        case "jwt"    => HookService.JwtAuthConfig
        case t        => throw WrongConfig(s"Unsupported hook type: $t")
      }
      HookConfig(
        config.getString("baseUrl"),
        getListConfig(config, "enabled"),
        authConfig
      )
    }.fold(logAndThrow("Hook"), identity)
  }

  override implicit lazy val getAmqpConfig: AmqpConfig = {
    Try {
      val config = conf.getConfig("amqp")
      AmqpConfig(
        readFromFileOrConf(config, "uri")
      )
    }.fold(logAndThrow("AMPQ"), identity)
  }

  override implicit lazy val emailServiceFactoryConfig: EmailServiceFactoryConfig = {
    Try {
      val config = conf.getConfig("userpass.email")
      EmailServiceFactoryConfig(
        config.getString("type")
      )
    }.fold(logAndThrow("E-mail service type selector"), identity)
  }

  override implicit lazy val passwordSettings: PasswordSettings = {
    Try {
      val config = conf.getConfig("userpass.passwordDifficulty")
      PasswordSettings(
        config.getString("pattern")
      )
    }.fold(logAndThrow("Password difficulty"), identity)
  }

  override implicit lazy val ldapConfig: LdapConfig = {
    Try {
      val config = conf.getConfig("ldap")
      LdapConfig(
        config.getString("url"),
        config.getString("readonlyUserWithNamespace"),
        readFromFileOrConf(config, "readonlyUserPassword"),
        config.getString("userSearchBaseDomain"),
        config.getString("userSearchAttribute"),
        config.getString("userSearchReturnAttributes").split(",").toSeq,
        config.getString("userSearchReturnArrayAttributes").split(",").toSeq
      )
    }.fold(logAndThrow("LDAP"), identity)
  }
  override implicit lazy val totpConfig: TotpConfig = {
    Try {
      val config = conf.getConfig("totp")
      val algo   = config.getString("algorithm")
      OTPAlgorithm.algos
        .find(_.name == algo)
        .fold {
          throw WrongConfig(s"Unsupported TOTP algo: $algo")
        } { _ =>
          TotpConfig(
            algo,
            config.getInt("window"),
            config.getDuration("period").toMillis / 1000 toInt,
            config.getInt("digits"),
            config.getBoolean("startFromCurrentTime")
          )
        }
    }.fold(logAndThrow("TOTP"), identity)
  }

  override implicit def getAmqpQueueConfig(name: String): AmqpQueueConfig = {
    Try {
      val path: String = name match {
        case "email_service" => "userpass.email.amqp"
        case _               => throw new IllegalArgumentException(s"$name unknown AMQP type.")
      }
      AmqpQueueConfig(conf.getConfig(path))
    }.fold(logAndThrow(s"AMQP for $name"), identity)
  }

  override implicit def escherConfig: EscherConfig = {
    Try {
      new EscherConfig(conf.getConfig("escher"))
    }.fold(logAndThrow("Escher"), identity)
  }

  override lazy val facebookConfig: OAuth2Config = readOauth2Config("facebook")
  override lazy val githubConfig: OAuth2Config   = readOauth2Config("github")
  override lazy val googleConfig: OAuth2Config   = readOauth2Config("google")

  override implicit lazy val jaegerConfig: JaegerConfig = {
    Try {
      val config = conf.getConfig("jaeger")
      val samplerConfig = SamplerConfiguration
        .fromEnv()
        .withType(config.getString("sampler.type"))
        .withParam(config.getInt("sampler.param"))
      val reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(config.getBoolean("reporter.withLogSpans"))
      new Configuration(config.getString("serviceName")).withSampler(samplerConfig).withReporter(reporterConfig)
    }.fold(logAndThrow("Jaeger"), identity)
  }

  override implicit lazy val providerExpirationConfigs: Map[String, ExpirationConfig] = {
    Try {
      import scala.jdk.CollectionConverters._
      val config    = conf.getConfig("providerLoginExpiration")
      val providers = config.root().keySet().asScala.map(_.toLowerCase)
      providers.map { providerName =>
        val expirationConfig = config.getString(s"$providerName.type") match {
          case ""           => ExpirationDisabled
          case "inactivity" => ExpirationInactivity(getDuration(config, s"$providerName.duration"))
          case "lifetime"   => ExpirationLifetime(getDuration(config, s"$providerName.duration"))
          case t            => throw new IllegalArgumentException(s"$t unknown expiration type.")
        }
        providerName -> expirationConfig
      }.toMap
    }.fold(logAndThrow("ExpirationService"), identity)
  }

  override implicit def getServiceAuthConfig(path: String): ServiceAuthDirectives.ServiceAuthConfig = {
    Try {
      val config   = conf.getConfig(path)
      val authType = config.getString("auth")
      authType match {
        case "basic" =>
          ServiceAuthDirectives.BasicAuthConfig(
            config.getString("basic.username"),
            readFromFileOrConf(config, "basic.password")
          )
        case "escher" =>
          ServiceAuthDirectives.EscherAuthConfig(
            config.getString("escher.trustedServices").split(",").toList
          )
        case "jwt" =>
          val configName = config.getString("jwt.configName")
          JwtAuthConfig(getJwtConfigByName(configName))
        case "blocked" =>
          AccessBlocked
        case t => throw new IllegalArgumentException(s"$t unknown service auth type in $path.")
      }
    }.fold(logAndThrow(s"ServiceAuth in $path"), identity)
  }

  private def readOauth2Config(name: String): OAuth2Config = {
    Try {
      val config = conf.getConfig(s"oauth2.$name")
      OAuth2Config(
        conf.getString("oauth2.rootUrl"),
        config.getString("clientId"),
        readFromFileOrConf(config, "clientSecret"),
        config.getString("scopes").split(",").toSeq
      )
    }.fold(logAndThrow(s"OAuth2 for $name"), identity)
  }

  private def readFromFileOrConf(config: Config, key: String): String = {
    lazy val fromConfig = config.getString(key)
    val fromFile        = Using(Source.fromFile(config.getString(s"${key}File")))(_.mkString)
    fromFile.getOrElse(fromConfig)
  }

  private def getDuration(config: Config, path: String) = {
    FiniteDuration(config.getDuration(path).getSeconds, TimeUnit.SECONDS)
  }

  private def logAndThrow(msg: String)(ex: Throwable) = {
    logger.error(s"WRONG CONFIG! $msg", ex)
    throw ex
  }

  override implicit def emailServiceSmtpConfig: EmailServiceSmtpConfig = {
    Try {
      val config = conf.getConfig(s"userpass.email.smtp")
      val smtpConfig: SmtpConfig = SmtpConfig(
        config.getString("host"),
        config.getInt("port"),
        config.getBoolean("ssl"),
        config.getString("username"),
        config.getString("password")
      )
      val templateConfig = EmailTemplateConfig(
        config.getString("template.senderAddress"),
        config.getString("template.registerTitle"),
        config.getString("template.registerBody"),
        config.getString("template.resetPasswordTitle"),
        config.getString("template.resetPasswordBody")
      )
      EmailServiceSmtpConfig(smtpConfig, templateConfig)
    }.fold(logAndThrow(s"EmailService SMTP"), identity)
  }

  private def getListConfig(conf: Config, key: String): Seq[String] = {
    conf
      .getString(key)
      .split(',')
      .map(_.trim.toLowerCase)
      .filterNot(_.isEmpty)
      .toSeq
  }

  override val getConfigForAkka: Config = conf
}
