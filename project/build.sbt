// Include plugin source in the meta-build so it applies itself to this project.
// The meta-build runs under sbt 1.x (Scala 2.12), so it uses the shared sources
// plus the scala-2.12 axis-specific variants (sbt-welcome + external sbt-projectmatrix).
Compile / unmanagedSourceDirectories ++= Seq(
  baseDirectory.value / ".." / "src" / "main" / "scala",
  baseDirectory.value / ".." / "src" / "main" / "scala-2.12"
)
