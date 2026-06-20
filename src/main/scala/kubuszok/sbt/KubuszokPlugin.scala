package kubuszok.sbt

import sbt.*
import sbt.Keys.*
import com.github.sbt.git.GitPlugin
import com.github.sbt.git.GitVersioning
import com.github.sbt.git.GitBranchPrompt
import com.github.sbt.git.SbtGit.git
import commandmatrix.CommandMatrixPlugin
import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbtide.Keys.ideSkipProject
import com.jsuereth.sbtpgp.SbtPgp
import com.jsuereth.sbtpgp.PgpKeys.publishSigned

object KubuszokPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires =
    GitPlugin && ScalafmtPlugin && CommandMatrixPlugin && SbtPgp

  // Provides "ci-release" command that:
  // - snapshot (no git tag): runs publishSigned (goes directly to snapshot repo)
  // - release (git tag present): runs publishSigned then sonatypeBundleRelease (stages locally, then pushes)
  private val ciReleaseCommand = Command.command("ci-release") { state =>
    val extracted = Project.extract(state)
    val tags = extracted.get(git.gitCurrentTags)
    if (tags.nonEmpty) "publishSigned" :: "sonaRelease" :: state
    else "publishSigned" :: state
  }

  object autoImport {

    lazy val isCI: Boolean = sys.env.get("CI").contains("true")

    val mavenCentralSnapshots =
      "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"

    def foldVersion[A](scalaVersion: String)(for2_13: => Seq[A], for3: => Seq[A]): Seq[A] =
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 13)) => for2_13
        case Some((3, _))  => for3
        case _             => Seq.empty
      }

    sealed trait ProjectType extends Product with Serializable
    object ProjectType {
      case object JarOnly extends ProjectType
      case object ScalaLibrary extends ProjectType
      case object NonPublished extends ProjectType
    }

    lazy val projectType: SettingKey[ProjectType] =
      settingKey[ProjectType]("What kind of project is this? Used to automatically configure publishing.")

    lazy val checkNoSnapshotDeps: TaskKey[Unit] =
      taskKey[Unit](
        "Fail the build if a non-snapshot (releasable) version depends on any -SNAPSHOT module (transitive included). No-op for SNAPSHOT versions."
      )
  }

  import autoImport.*

  // ThisProject / settings
  override lazy val buildSettings: Seq[Setting[?]] =
    GitVersioning.buildSettings ++ Seq(
      scalafmtOnCompile := !isCI
    )

  // Global / settings
  override lazy val globalSettings: Seq[Setting[?]] =
    GitBranchPrompt.globalSettings ++ Seq(
      commands += ciReleaseCommand,
      excludeLintKeys += git.useGitDescribe,
      excludeLintKeys += ideSkipProject,
      resolvers += mavenCentralSnapshots
    )

  // settings loaded only for a particular project
  override lazy val projectSettings: Seq[Setting[?]] =
    GitVersioning.projectSettings ++ GitBranchPrompt.projectSettings ++ Seq(
      git.useGitDescribe := true,
      git.uncommittedSignifier := None,
      Compile / doc / scalacOptions ++= foldVersion(scalaVersion.value)(
        for3 = Seq("-Ygenerate-inkuire"),
        for2_13 = Seq.empty
      ),
      Compile / console / scalacOptions --= Seq("-Ywarn-unused:imports", "-Werror", "-Xfatal-warnings"),
      publishTo := {
        if (isSnapshot.value) Some(mavenCentralSnapshots)
        else localStaging.value
      },
      publishMavenStyle := true,
      publish / skip := Compat.uncached(projectType.value == ProjectType.NonPublished),
      publishArtifact := Compat.uncached(projectType.value != ProjectType.NonPublished),
      Test / publishArtifact := false,
      pomIncludeRepository := { _ => false },
      versionScheme := Some("early-semver"),
      // Sonatype ignores isSnapshot setting and only looks at -SNAPSHOT suffix in version:
      //   https://central.sonatype.org/publish/publish-maven/#performing-a-snapshot-deployment
      // meanwhile sbt-git used to set up SNAPSHOT if there were uncommitted changes:
      //   https://github.com/sbt/sbt-git/issues/164
      // (now this suffix is empty by default) so we need to fix it manually.
      git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty,
      git.uncommittedSignifier := Some("SNAPSHOT"),
      projectType := ProjectType.NonPublished,
      checkNoSnapshotDeps := Compat.uncached {
        val log = streams.value.log
        // A SNAPSHOT version is allowed to depend on SNAPSHOTs.
        if (isSnapshot.value) {
          log.debug("checkNoSnapshotDeps: skipped (this is a SNAPSHOT version)")
        } else {
          val offending = update.value.allModules
            .filter(_.revision.endsWith("-SNAPSHOT"))
            .map(m => s"${m.organization}:${m.name}:${m.revision}")
            .distinct
            .sorted
          if (offending.nonEmpty) {
            sys.error(
              s"""Refusing to publish releasable version ${version.value}: it depends on SNAPSHOT modules:
                 |  ${offending.mkString("\n  ")}
                 |Release those dependencies first, or publish this as a SNAPSHOT.""".stripMargin
            )
          }
        }
      },
      // Run the SNAPSHOT guard before publishing (it short-circuits for SNAPSHOT versions).
      publish := publish.dependsOn(checkNoSnapshotDeps).value,
      publishSigned := publishSigned.dependsOn(checkNoSnapshotDeps).value
    )
}
