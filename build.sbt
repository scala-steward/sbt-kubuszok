import sbtwelcome.UsefulTask

val versions = new {

  val sbt1 = "1.12.12"
  val sbt2 = "2.0.0"
}

// The plugin sources are compiled by the meta-build (project/build.sbt) and apply themselves here.
lazy val sbtKubuszok = (project in file("."))
  .enablePlugins(SbtPlugin, KubuszokRootPlugin)
  .settings(
    name := "sbt-kubuszok",
    organization := "com.kubuszok",
    homepage := Some(url("https://github.com/kubuszok/sbt-kubuszok")),
    licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://kubuszok.com"))
    ),
    // Cross-build for sbt 1.x (Scala 2.12) and sbt 2.0 (Scala 3).
    // sbt 2.0.0 is itself built against Scala 3.8.4, so the plugin must compile
    // with 3.8.4 to read sbt's TASTy (3.7.2 cannot read 3.8.4 TASTy).
    crossScalaVersions := Seq("3.8.4", "2.12.21"),
    scalaVersion := "2.12.21",
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => versions.sbt1
        case _      => versions.sbt2
      }
    },
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked"
    ),
    projectType := ProjectType.JarOnly,
    // welcome
    logo := s"""sbt-kubuszok ${(version).value} build for (${versions.sbt1}, ${versions.sbt2})
               |""".stripMargin,
    usefulTasks := Seq(
      UsefulTask("compile", "Compile the plugin").noAlias,
      UsefulTask("publishLocal", "Publish locally for testing in other projects").noAlias,
      UsefulTask("ci-release", "Publish snapshot or release (based on git tags)").noAlias,
      UsefulTask("scalafmtAll", "Format all sources").noAlias
    ),
    // Plugin dependencies (transitive for users of sbt-kubuszok)
    // git
    addSbtPlugin("com.github.sbt" % "sbt-git" % "2.1.0"),
    // linters
    addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1"),
    addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4"),
    // cross-compile
    addSbtPlugin("com.indoorvivants" % "sbt-commandmatrix" % "0.1.0"),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0"),
    addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12"),
    // publishing
    addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1"),
    // MiMa
    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.6"),
    // disabling projects in IDE
    addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.4"),
    // sbt-1.x-only plugins:
    //  - sbt-projectmatrix: merged INTO sbt 2.0 (built-in there), so add only on the sbt-1.x axis
    //  - sbt-welcome: has no final sbt-2.0 build, so add only on the sbt-1.x axis
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "2.12") {
        val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
        val scalaV = (update / scalaBinaryVersion).value
        Seq(
          // cross-compile
          Defaults.sbtPluginExtra("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0", sbtV, scalaV),
          // documentation
          Defaults.sbtPluginExtra("com.github.reibitto" % "sbt-welcome" % "0.5.0", sbtV, scalaV)
        )
      } else Seq.empty
    }
  )
