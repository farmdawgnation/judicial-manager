name := "scyig-judicial"

organization := "me.frmr.scyig"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-actor" % "3.0",
  "net.liftweb" %% "lift-util" % "3.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)
