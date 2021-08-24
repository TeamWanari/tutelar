import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

lazy val ver = version := "1.0.0-SNAPSHOT"

lazy val commonSettings = Seq(
  scalaVersion      := "2.13.5",
  organization      := "com.wanari",
  scalafmtOnCompile := true,
  ver
)

lazy val ItTest         = config("it") extend Test
lazy val itTestSettings = Defaults.itSettings ++ scalafmtConfigSettings

lazy val remoteRepo = sys.env
  .get("GITHUB_TOKEN")
  .fold("git@github.com:TeamWanari/tutelar.git")(token =>
    s"https://x-access-token:$token@github.com/TeamWanari/tutelar.git"
  )
lazy val docs = (project in file("docs"))
  .settings(
    name                       := "paradox-docs",
    paradoxTheme               := Some(builtinParadoxTheme("generic")),
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

lazy val root = (project in file("."))
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
        "org.typelevel"        %% "cats-core"                  % "2.6.1",
        "com.typesafe.akka"    %% "akka-http"                  % "10.2.6",
        "com.typesafe.akka"    %% "akka-http-spray-json"       % "10.2.6",
        "com.typesafe.akka"    %% "akka-http-testkit"          % "10.2.6"  % "it,test",
        "com.typesafe.akka"    %% "akka-actor"                 % "2.6.16",
        "com.typesafe.akka"    %% "akka-stream"                % "2.6.16",
        "com.typesafe.akka"    %% "akka-slf4j"                 % "2.6.16",
        "com.typesafe.akka"    %% "akka-testkit"               % "2.6.16"  % "it,test",
        "ch.qos.logback"        % "logback-classic"            % "1.2.5",
        "net.logstash.logback"  % "logstash-logback-encoder"   % "6.6",
        "org.slf4j"             % "jul-to-slf4j"               % "1.7.32",
        "com.typesafe.slick"   %% "slick"                      % "3.3.3",
        "com.typesafe.slick"   %% "slick-hikaricp"             % "3.3.3",
        "org.postgresql"        % "postgresql"                 % "42.2.23",
        "com.github.jwt-scala" %% "jwt-core"                   % "9.0.1",
        "com.github.jwt-scala" %% "jwt-spray-json"             % "9.0.1",
        "org.mindrot"           % "jbcrypt"                    % "0.4",
        "commons-codec"         % "commons-codec"              % "1.15",
        "ch.megard"            %% "akka-http-cors"             % "1.1.2",
        "io.opentracing"        % "opentracing-api"            % "0.33.0",
        "io.opentracing"        % "opentracing-util"           % "0.33.0",
        "io.opentracing"        % "opentracing-noop"           % "0.33.0",
        "io.jaegertracing"      % "jaeger-client"              % "1.6.0",
        "org.reactivemongo"    %% "reactivemongo"              % "1.0.6",
        "org.reactivemongo"    %% "reactivemongo-bson-monocle" % "1.0.6",
        "com.lightbend.akka"   %% "akka-stream-alpakka-amqp"   % "3.0.3",
        "org.bouncycastle"      % "bcprov-jdk15on"             % "1.69",
        "com.emarsys"          %% "escher-akka-http"           % "1.3.8",
        "org.codehaus.janino"   % "janino"                     % "3.1.6",
        "org.apache.commons"    % "commons-email"              % "1.5",
        "org.scalatest"        %% "scalatest"                  % "3.2.9"   % "it,test",
        "org.mockito"          %% "mockito-scala"              % "1.16.37" % "it,test"
      )
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", "test it:test")

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full)
addCompilerPlugin("io.tryp"        % "splain"         % "0.5.8" cross CrossVersion.patch)

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
