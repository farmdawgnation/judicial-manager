name := "judicial-manager"

organization := "me.frmr.scyig"

version := "1.1.0-SNAPSHOT"

scalaVersion := "2.12.3"

scalacOptions in Compile ++= Seq("-feature", "-deprecation")

libraryDependencies ++= {
  val liftVersion = "3.2.0-M2"

  Seq(
    "net.liftweb"                   %% "lift-webkit"          % liftVersion,
    "com.typesafe.slick"            %% "slick"                % "3.2.1",
    "mysql"                         % "mysql-connector-java"  % "8.0.7-dmr",
    "org.flywaydb"                  % "flyway-core"           % "4.2.0",
    "ch.qos.logback"                % "logback-classic"       % "1.2.3",
    "org.apache.commons"            % "commons-csv"           % "1.5",
    "org.scalatest"                 %% "scalatest"            % "3.0.1" % "test",
    "org.scalacheck"                %% "scalacheck"           % "1.13.4" % "test"
  )
}

enablePlugins(JettyPlugin)

webappPostProcess := { webappDir: File =>
  def recurseFiles(rootDir: File, targetDir: File, extension: String, handler: (String, String, String)=>Unit): Unit = {
    if (! rootDir.isDirectory || ! targetDir.isDirectory) {
      streams.value.log.error(s"$rootDir and $targetDir must both be directories")
    } else {
      for {
        file <- rootDir.listFiles if file.getName.endsWith(extension) || file.isDirectory
      } {
        if (file.isDirectory) {
          if (! (targetDir / file.getName).isDirectory)
            (targetDir / file.getName).mkdir

          recurseFiles(
            rootDir / file.getName,
            targetDir / file.getName,
            extension,
            handler
          )
        } else if (! file.getName.startsWith("_")) {
          streams.value.log.info(s"Processing ${file.getPath}...")
          handler(
            rootDir.toString,
            file.getName,
            targetDir.toString
          )
        }
      }
    }
  }

  val scssLibPath = webappDir / "scss-lib-hidden"
  def compileScss(inputDir: String, inputFile: String, outputDir: String): Unit = {
    val outputFilename = inputFile.replace(".scss", ".css")
    s"scss -q -I $scssLibPath $inputDir/$inputFile $outputDir/$outputFilename" ! streams.value.log
  }

  recurseFiles(
    webappDir / "scss-hidden",
    webappDir / "css",
    ".scss",
    compileScss _
  )
}

artifactName := { (v: ScalaVersion, m: ModuleID, a: Artifact) =>
  a.name + "." + a.extension
}
