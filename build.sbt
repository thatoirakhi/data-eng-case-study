ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "pipeline",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.4",
      "org.apache.spark" %% "spark-sql" % "3.5.4",
      "org.apache.spark" %% "spark-hadoop-cloud" % "3.5.4",
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.github.pureconfig" %% "pureconfig" % "0.15.0",
      "org.scalatest" %% "scalatest" % "3.2.17"
    )
  )
