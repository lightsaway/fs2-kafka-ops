import Dependencies.{mockito, _}

lazy val `fs2-kafka-ops` = (project in file("."))
  .settings(
    organizationName := "lightsaway",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalacOptions ++= ScalacOptions.default ,
    inThisBuild(
      List(
        scalaVersion := "2.12.6",
        scalafmtOnCompile := true,
        testOptions in Test += Tests.Argument("-oF"),
        javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
        version ~= (_.replace('+', '-')),
        dynver ~= (_.replace('+', '-')),
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
    scalafmtOnCompile := true
  )
