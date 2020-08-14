import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

lazy val ver = version := "1.0.0-SNAPSHOT"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.3",
  organization := "com.wanari",
  scalafmtOnCompile := true,
  ver
)

lazy val ItTest         = config("it") extend Test
lazy val itTestSettings = Defaults.itSettings ++ scalafmtConfigSettings

lazy val root = project
  .in(file("."))
  .aggregate(core)

lazy val remoteRepo = sys.env
  .get("GITHUB_TOKEN")
  .fold("git@github.com:TeamWanari/tutelar.git")(token =>
    s"https://x-access-token:$token@github.com/TeamWanari/tutelar.git"
  )
lazy val docs = (project in file("docs"))
  .settings(
    name := "paradox-docs",
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox",
    scmInfo := Some(
      ScmInfo(url("https://github.com/TeamWanari/tutelar"), "scm:git:git@github.com:TeamWanari/tutelar.git")
    ),
    git.remoteRepo := remoteRepo,
    ver
  )
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ParadoxSitePlugin)
  .enablePlugins(GhpagesPlugin)

lazy val core = (project in file("."))
  .configs(ItTest)
  .settings(inConfig(ItTest)(itTestSettings): _*)
  .settings(commonSettings: _*)
  .settings(buildInfoSettings: _*)
  .settings(
    name := "tutelar",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-Ywarn-dead-code",
      "-Xlint"
    ),
    libraryDependencies ++= {
      Seq(
        "org.typelevel"       %% "cats-core"                  % "2.1.1",
        "com.typesafe.akka"   %% "akka-http"                  % "10.2.0",
        "com.typesafe.akka"   %% "akka-http-spray-json"       % "10.2.0",
        "com.typesafe.akka"   %% "akka-http-testkit"          % "10.2.0" % "it,test",
        "com.typesafe.akka"   %% "akka-actor"                 % "2.6.8",
        "com.typesafe.akka"   %% "akka-stream"                % "2.6.8",
        "com.typesafe.akka"   %% "akka-slf4j"                 % "2.6.8",
        "com.typesafe.akka"   %% "akka-testkit"               % "2.6.8"  % "it,test",
        "ch.qos.logback"       % "logback-classic"            % "1.2.3",
        "net.logstash.logback" % "logstash-logback-encoder"   % "6.4",
        "org.slf4j"            % "jul-to-slf4j"               % "1.7.30",
        "com.typesafe.slick"  %% "slick"                      % "3.3.2",
        "com.typesafe.slick"  %% "slick-hikaricp"             % "3.3.2",
        "org.postgresql"       % "postgresql"                 % "42.2.15",
        "com.pauldijou"       %% "jwt-core"                   % "4.3.0",
        "com.pauldijou"       %% "jwt-spray-json"             % "4.3.0",
        "org.mindrot"          % "jbcrypt"                    % "0.4",
        "commons-codec"        % "commons-codec"              % "1.14",
        "ch.megard"           %% "akka-http-cors"             % "1.1.0",
        "io.opentracing"       % "opentracing-api"            % "0.33.0",
        "io.opentracing"       % "opentracing-util"           % "0.33.0",
        "io.opentracing"       % "opentracing-noop"           % "0.33.0",
        "io.jaegertracing"     % "jaeger-client"              % "1.3.2",
        "org.reactivemongo"   %% "reactivemongo"              % "0.20.12-fix1",
        "org.reactivemongo"   %% "reactivemongo-bson-monocle" % "0.20.12",
        "com.lightbend.akka"  %% "akka-stream-alpakka-amqp"   % "2.0.1",
        "org.bouncycastle"     % "bcprov-jdk15on"             % "1.66",
        "com.emarsys"         %% "escher-akka-http"           % "1.2.0",
        "org.codehaus.janino"  % "janino"                     % "3.1.2",
        "org.apache.commons"   % "commons-email"              % "1.5",
        "org.scalatest"       %% "scalatest"                  % "3.2.1"  % "it,test",
        "org.mockito"         %% "mockito-scala"              % "1.14.8" % "it,test"
      )
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", "test it:test")

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("io.tryp"        % "splain"         % "0.5.7" cross CrossVersion.patch)

cancelable in Global := true

lazy val buildTime                       = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
lazy val builtAtMillis: SettingKey[Long] = SettingKey[Long]("builtAtMillis", "time of build")
ThisBuild / builtAtMillis := buildTime.toInstant.toEpochMilli
lazy val builtAtString: SettingKey[String] = SettingKey[String]("builtAtString", "time of build")
ThisBuild / builtAtString := buildTime.toString

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    BuildInfoKey.action("commitHash") {
      git.gitHeadCommit.value
    },
    builtAtString,
    builtAtMillis
  ),
  buildInfoPackage := "com.wanari.tutelar"
)
