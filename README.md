# sbt-kubuszok

An sbt plugin that bundles and auto-configures common build settings for Scala projects using sbt-projectmatrix.

## Installation

Add to `project/plugins.sbt`:

```scala
addSbtPlugin("com.kubuszok" % "sbt-kubuszok" % "0.2.0")
```

This single plugin brings in and configures:

- [sbt-git](https://github.com/sbt/sbt-git) (git-based versioning)
- [sbt-scalafmt](https://github.com/scalameta/sbt-scalafmt) (code formatting)
- [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) (code coverage)
- [sbt-projectmatrix](https://github.com/sbt/sbt-projectmatrix) (cross-building)
- [sbt-commandmatrix](https://github.com/indoorvivants/sbt-commandmatrix) (CI commands)
- [sbt-scalajs](https://github.com/nicknux/sbt-scalajs) (Scala.js)
- [sbt-scala-native](https://github.com/nicknux/sbt-scala-native) (Scala Native)
- [sbt-pgp](https://github.com/sbt/sbt-pgp) (artifact signing)
- [sbt-mima-plugin](https://github.com/lightbend/mima) (binary compatibility)
- [sbt-ide-settings](https://github.com/JetBrains/sbt-ide-settings) (IDE project filtering)
- [sbt-welcome](https://github.com/reibitto/sbt-welcome) (useful tasks display)

## What it provides

### KubuszokPlugin (auto-triggered)

Activates automatically when its dependencies are present. Provides:

- **Git versioning**: Enables `GitVersioning` and `GitBranchPrompt` for all projects. Version is derived from git tags (`v1.0.0` tag -> `1.0.0`, no tag -> `<hash>-SNAPSHOT`).
- **Publishing**: Configures `publishTo` (snapshots to Maven Central Snapshots, releases to local staging), `publishMavenStyle`, `versionScheme := "early-semver"`.
- **`ci-release` command**: Dynamically checks git tags at runtime — if a tag is present, runs `publishSigned` then `sonatypeBundleRelease`; otherwise just `publishSigned` (snapshot).
- **`projectType` setting**: Controls publishing behavior per project:
  - `ProjectType.ScalaLibrary` — published (default for libraries)
  - `ProjectType.JarOnly` — published (for plugins/tools)
  - `ProjectType.NonPublished` — skipped during publish
- **`scalafmtOnCompile`**: Enabled in dev, disabled on CI (detected via `CI=true` env var).
- **`foldVersion` helper**: Pattern match on Scala binary version:
  ```scala
  scalacOptions ++= foldVersion(scalaVersion.value)(
    for2_13 = Seq("-Ytasty-reader"),
    for3 = Seq("-Xcheck-macros")
  )
  ```
- **Resolver**: Maven Central Snapshots added globally.

### KubuszokRootPlugin (manual trigger)

Enable on the root/aggregation project to get `WelcomePlugin`:

```scala
lazy val root = project
  .in(file("."))
  .enablePlugins(KubuszokRootPlugin)
  .settings(
    logo := s"My Project ${version.value}",
    usefulTasks := al.usefulTasks(...)
  )
```

### DevProperties

Configures IDE visibility for multi-platform/multi-version project matrix builds. Reads from `dev.properties`:

```scala
val dev = new DevProperties(
  scala213 = Some(versions.scala213),
  scala3 = Some(versions.scala3),
  platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)
)

// Use in project matrix definitions:
lazy val myLib = projectMatrix
  .someVariations(versions.scalas, versions.platforms)(dev.only1VersionInIDE *)
```

`dev.properties` (gitignored, per-developer):
```properties
ide.scala=3
ide.platform=jvm
```

### Aliases

Derives CI/test/publish commands from `ProjectMatrix` instances grouped by role:

- **`published`** — compiled, tested, and published (included in `publishLocal` and `publishLocalForTests`)
- **`testOnly`** — compiled and tested, but not published
- **`compileOnly`** — only verified for compilation

Modules are deduplicated across groups, so accidental overlap won't produce duplicate tasks.

```scala
lazy val al = new Aliases(
  published = Seq(core, utils, integration),
  testOnly = Seq(tests),
  compileOnly = Seq(benchmarks)
)

// Auto-generated commands for each (platform, scala) combination:
al.ci("JVM", "3")       // clean ; coverage ; core3/compile ; ... ; coverageOff
al.test("JS", "2.13")   // coreJS/test ; utilsJS/test ; testsJS/test ; ...
al.release              // "ci-release" (the built-in command)
al.publishLocal("JVM", "2.13")

// Generate sbt-welcome tasks automatically:
usefulTasks := al.usefulTasks(
  publishLocalForTestsFilter = Some((_, platform) => platform == "JVM"),
  extra = Seq(
    UsefulTask("myCustomCommand", "Does something special").alias("custom")
  )
)
```

Subclass for project-specific needs:

```scala
lazy val al = new Aliases(published = Seq(core, utils, tests)) {
  override def ci(platform: String, scalaBinary: String): String = {
    val base = super.ci(platform, scalaBinary)
    val mimaIds = projectIds(published, platform, scalaBinary)
    s"$base ; ${mimaIds.map(id => s"$id/mimaReportBinaryIssues").mkString(" ; ")}"
  }
}
```

## Self-bootstrapping

The plugin uses itself for its own build via `project/build.sbt`:

```scala
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "src" / "main" / "scala"
```

This compiles the plugin source in the meta-build, so the same code is both published as an sbt plugin and used to configure its own publishing.

## License

Apache 2.0 - see [LICENSE](LICENSE).
