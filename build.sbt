name := "scyig-judicial"

organization := "me.frmr.scyig"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions in Compile ++= Seq("-feature")

libraryDependencies ++= {
  val liftVersion = "3.0"

  Seq(
    "net.liftweb" %% "lift-webkit" % "3.0.1",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
  )
}

enablePlugins(JettyPlugin)
