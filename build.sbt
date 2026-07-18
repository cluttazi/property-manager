name := """property-manager"""

scalaVersion := "2.13.18"

lazy val slick = "3.6.1"
lazy val h2 = "2.3.232"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "org.playframework" %% "play-slick" % "6.2.0",
  "org.playframework" %% "play-slick-evolutions" % "6.2.0",
  "com.typesafe.slick" %% "slick" % slick,
  "com.typesafe.slick" %% "slick-hikaricp" % slick,
  "com.h2database" % "h2" % h2,
  specs2 % Test,
  evolutions
)
