
name := """property-manager"""
scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.12.4", "2.11.12")

lazy val slick = "3.2.1"
lazy val h2 = "1.4.196"
lazy val play = "2.6.13"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-slick" % "3.0.3",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",
  "org.fluentlenium" % "fluentlenium-core" % "3.2.0",
  "com.typesafe.play" %% "play-jdbc-api" % play,
  "com.typesafe.play" %% "play-jdbc-evolutions" % play,
  "com.typesafe.play" %% "play-specs2" % play,
  "com.typesafe.slick" %% "slick" % slick,
  "com.typesafe.slick" %% "slick-hikaricp" % slick,
  "com.h2database" % "h2" % h2,
  specs2 % Test,
  evolutions
)