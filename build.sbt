name := "scyig-judicial"

organization := "me.frmr.scyig"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.2"

scalacOptions in Compile ++= Seq("-feature", "-deprecation")

libraryDependencies ++= {
  val liftVersion = "3.1.0"

  Seq(
    "net.liftweb"                   %% "lift-webkit"          % liftVersion,
    "com.typesafe.slick"            %% "slick"                % "3.2.0",
    "mysql"                         % "mysql-connector-java"  % "8.0.7-dmr",
    "ch.qos.logback"                % "logback-classic"       % "1.2.3",
    "org.scalatest"                 %% "scalatest"            % "3.0.1" % "test",
    "org.scalacheck"                %% "scalacheck"           % "1.13.4" % "test"
  )
}

enablePlugins(JettyPlugin)
