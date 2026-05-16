package kubuszok.sbt

import sbt.*
import sbt.Keys.*
import sbt.VirtualAxis
import commandmatrix.extra.MatrixAction
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbtide.Keys.ideSkipProject

class DevProperties(
    val scala213: Option[String],
    val scala3: Option[String],
    val platforms: Seq[VirtualAxis.PlatformAxis]
) {

  lazy val props = scala.util
    .Using(new java.io.FileInputStream("dev.properties")) { fis =>
      val props = new java.util.Properties()
      props.load(fis)
      props
    }
    .getOrElse(new java.util.Properties())

  val ideScala: String = (scala213, scala3) match {
    case (Some(version), None) => version
    case (_, Some(version))    => version
    case _                     =>
      props.getProperty("ide.scala") match {
        case "2.13" => scala213.getOrElse(sys.error("scala213 version not configured but ide.scala=2.13"))
        case "3"    => scala3.getOrElse(sys.error("scala3 version not configured but ide.scala=3"))
        case _      => sys.error("Build not configured to use exactly 1 Scala version, ide.scala must be 2.13 or 3")
      }
  }

  val idePlatform: VirtualAxis.PlatformAxis = platforms match {
    case Seq()         => VirtualAxis.jvm
    case Seq(platform) => platform
    case _             =>
      props.getProperty("ide.platform") match {
        case "jvm"    => VirtualAxis.jvm
        case "js"     => VirtualAxis.js
        case "native" => VirtualAxis.native
        case _ => sys.error("Build not configured to use exactly 1 platform, ide.platform must be jvm, js, or native")
      }
  }

  def isIdeScala(scalaVersion: String): Boolean =
    CrossVersion.partialVersion(scalaVersion) == CrossVersion.partialVersion(ideScala)

  def isIdePlatform(platform: VirtualAxis.PlatformAxis): Boolean =
    platform == idePlatform

  def only1VersionInIDE: Seq[MatrixAction.Act] =
    MatrixAction
      .ForPlatform(idePlatform)
      .Configure(
        _.settings(
          ideSkipProject := !isIdeScala(scalaVersion.value),
          bspEnabled := isIdeScala(scalaVersion.value),
          scalafmtOnCompile := !KubuszokPlugin.autoImport.isCI
        )
      ) +:
      platforms.filterNot(isIdePlatform).map { platform =>
        MatrixAction
          .ForPlatform(platform)
          .Configure(
            _.settings(ideSkipProject := true, bspEnabled := false, scalafmtOnCompile := false)
          )
      }
}
