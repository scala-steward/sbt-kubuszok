// Include plugin source in the meta-build so it applies itself to this project.
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "src" / "main" / "scala"
