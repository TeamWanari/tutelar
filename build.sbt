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
      val akkaV     = "2.5.21"
      val slickV    = "3.3.0"
      val jwtV      = "2.1.0"
      Seq(
        "org.typelevel"        %% "cats-core"               % "1.6.0",
        "com.typesafe.akka"    %% "akka-http"               % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-spray-json"    % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-testkit"       % akkaHttpV % "it,test",
        "com.typesafe.akka"    %% "akka-actor"              % akkaV,
        "com.typesafe.akka"    %% "akka-stream"             % akkaV,
        "com.typesafe.akka"    %% "akka-slf4j"              % akkaV,
        "com.typesafe.akka"    %% "akka-testkit"            % akkaV % "it,test",
        "ch.qos.logback"       % "logback-classic"          % "1.2.3",
        "net.logstash.logback" % "logstash-logback-encoder" % "5.3",
        "org.slf4j"            % "jul-to-slf4j"             % "1.7.26",
        "com.typesafe.slick"   %% "slick"                   % slickV,
        "com.typesafe.slick"   %% "slick-hikaricp"          % slickV,
        "org.postgresql"       % "postgresql"               % "42.2.5",
        "com.pauldijou"        %% "jwt-core"                % jwtV,
        "com.pauldijou"        %% "jwt-spray-json"          % jwtV,
        "org.mindrot"          % "jbcrypt"                  % "0.4",
        "commons-codec"        % "commons-codec"            % "1.12",
        "ch.megard"            %% "akka-http-cors"          % "0.4.0",
        "io.opentracing"       % "opentracing-api"          % "0.32.0",
        "io.opentracing"       % "opentracing-util"         % "0.32.0",
        "io.opentracing"       % "opentracing-noop"         % "0.32.0",
        "io.opentracing"       % "opentracing-mock"         % "0.32.0",
        "io.jaegertracing"     % "jaeger-client"            % "0.32.0",
        "org.scalatest"        %% "scalatest"               % "3.0.5" % "it,test",
        "org.mockito"          % "mockito-core"             % "2.25.1" % "it,test",
        "org.mockito"          %% "mockito-scala"           % "1.2.1" % "it,test"
      )
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", "test it:test")

enablePlugins(JavaAppPackaging)
enablePlugins(BuildInfoPlugin)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
addCompilerPlugin("io.tryp"        % "splain"          % "0.4.0" cross CrossVersion.patch)

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
