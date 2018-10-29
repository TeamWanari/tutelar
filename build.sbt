import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings

lazy val commonSettings = Seq(
  scalaVersion := "2.12.7",
  organization := "com.wanari",
  scalafmtOnCompile := true
)

lazy val ItTest         = config("it") extend Test
lazy val itTestSettings = Defaults.itSettings ++ scalafmtConfigSettings

lazy val root = (project in file("."))
  .configs(ItTest)
  .settings(inConfig(ItTest)(itTestSettings): _*)
  .settings(commonSettings: _*)
  .settings(
    name := "tutelar",
    version := "0.1.0",
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
      val akkaHttpV  = "10.1.5"
      val akkaV      = "2.5.12"
      val scalaTestV = "3.0.5"
      Seq(
        "org.typelevel"        %% "cats-core"               % "1.4.0",
        "com.typesafe.akka"    %% "akka-http"               % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-spray-json"    % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-testkit"       % akkaHttpV % "it,test",
        "com.typesafe.akka"    %% "akka-actor"              % akkaV,
        "com.typesafe.akka"    %% "akka-stream"             % akkaV,
        "com.typesafe.akka"    %% "akka-slf4j"              % akkaV,
        "ch.qos.logback"       % "logback-classic"          % "1.2.3",
        "net.logstash.logback" % "logstash-logback-encoder" % "5.2",
        "org.slf4j"            % "jul-to-slf4j"             % "1.7.25",
        "org.scalatest"        %% "scalatest"               % scalaTestV % "it,test",
        "org.mockito"          % "mockito-core"             % "2.23.0" % "it,test",
        "com.typesafe.slick"   %% "slick"                   % "3.2.3",
        "com.typesafe.slick"   %% "slick-hikaricp"          % "3.2.3",
        "org.postgresql"       % "postgresql"               % "42.2.5",
        "com.pauldijou"        %% "jwt-core"                % "0.19.0",
        "com.pauldijou"        %% "jwt-spray-json"          % "0.19.0"
      )
    }
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt it:scalafmt")
addCommandAlias("testAll", "; test ; it:test")

enablePlugins(JavaAppPackaging)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")
addCompilerPlugin("io.tryp"        % "splain"          % "0.3.4" cross CrossVersion.patch)

import com.typesafe.sbt.packager.docker._
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:8"
dockerCommands ++= Seq(
  Cmd("ARG", "BUILD_VERSION"),
  Cmd("ENV", "VERSION=$BUILD_VERSION")
)
