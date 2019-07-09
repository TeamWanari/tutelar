import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  organization := "com.wanari",
  scalafmtOnCompile := true,
  version := "0.1.0"
)

lazy val ItTest         = config("it") extend Test
lazy val itTestSettings = Defaults.itSettings ++ scalafmtConfigSettings

lazy val root = project
  .in(file("."))
  .aggregate(core)

lazy val docs = (project in file("docs"))
  .settings(
    name := "paradox-docs",
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"
  )
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ParadoxSitePlugin)

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
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Xlint"
    ),
    libraryDependencies ++= {
      val akkaHttpV = "10.1.8"
      val akkaV     = "2.5.23"
      val slickV    = "3.3.2"
      val jwtV      = "3.1.0"
      Seq(
        "org.typelevel"        %% "cats-core"                % "2.0.0-M4",
        "com.typesafe.akka"    %% "akka-http"                % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-spray-json"     % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-testkit"        % akkaHttpV % "it,test",
        "com.typesafe.akka"    %% "akka-actor"               % akkaV,
        "com.typesafe.akka"    %% "akka-stream"              % akkaV,
        "com.typesafe.akka"    %% "akka-slf4j"               % akkaV,
        "com.typesafe.akka"    %% "akka-testkit"             % akkaV % "it,test",
        "ch.qos.logback"       % "logback-classic"           % "1.2.3",
        "net.logstash.logback" % "logstash-logback-encoder"  % "6.1",
        "org.slf4j"            % "jul-to-slf4j"              % "1.7.26",
        "com.typesafe.slick"   %% "slick"                    % slickV,
        "com.typesafe.slick"   %% "slick-hikaricp"           % slickV,
        "org.postgresql"       % "postgresql"                % "42.2.6",
        "com.pauldijou"        %% "jwt-core"                 % jwtV,
        "com.pauldijou"        %% "jwt-spray-json"           % jwtV,
        "org.mindrot"          % "jbcrypt"                   % "0.4",
        "commons-codec"        % "commons-codec"             % "1.12",
        "ch.megard"            %% "akka-http-cors"           % "0.4.1",
        "io.opentracing"       % "opentracing-api"           % "0.33.0",
        "io.opentracing"       % "opentracing-util"          % "0.33.0",
        "io.opentracing"       % "opentracing-noop"          % "0.33.0",
        "io.jaegertracing"     % "jaeger-client"             % "0.35.5",
        "org.reactivemongo"    %% "reactivemongo"            % "0.18.1",
        "com.lightbend.akka"   %% "akka-stream-alpakka-amqp" % "1.1.0",
        "com.spingo"           %% "op-rabbit-core"           % "2.1.0",
        "org.bouncycastle"     % "bcprov-jdk15on"            % "1.62",
        "org.scalatest"        %% "scalatest"                % "3.0.8" % "it,test",
        "org.mockito"          % "mockito-core"              % "2.28.2" % "it,test",
        "org.mockito"          %% "mockito-scala"            % "1.5.11" % "it,test"
      )
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", "test it:test")

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
addCompilerPlugin("io.tryp"       % "splain"          % "0.4.1" cross CrossVersion.patch)

cancelable in Global := true

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    BuildInfoKey.action("commitHash") {
      git.gitHeadCommit.value
    }
  ),
  buildInfoOptions := Seq(BuildInfoOption.BuildTime),
  buildInfoPackage := "com.wanari.tutelar"
)
