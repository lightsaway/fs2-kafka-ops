import Dependencies.{mockito, _}

lazy val `fs2-kafka-ops` = (project in file("."))
  .settings(
    organization := "com.github.lightsaway",
    startYear := Some(2018),
    homepage := Some(url("https://github.com/lightsaway/fs2-kafka-ops")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "lightsaway",
        "Anton Serhiienko",
        "a.a.sergienko@gmail.com",
        url("https://lightsaway.com")
      )
    ),

    scalacOptions ++= ScalacOptions.default ,
    inThisBuild(
      List(
        scalaVersion := "2.12.6",
        scalafmtOnCompile := true,
        testOptions in Test += Tests.Argument("-oF"),
        javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
        parallelExecution := false
      )),
    name := "fs2-kafka-ops",
    libraryDependencies ++= Seq(
      fs2.core,
      kafka.clients,
      slf4j.api,
      slf4j.log4jOver,
      prometheusClient,
      logback.core % Test,
      logback.classic % Test,
      scalaTest % Test,
      embeddedKafka % Test,
      mockito.sugar % Test,
      mockito.core % Test
    ),
    scalafmtOnCompile := true,

  )
