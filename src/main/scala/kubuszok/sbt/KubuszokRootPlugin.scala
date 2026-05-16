package kubuszok.sbt

import sbt._
import sbtwelcome.WelcomePlugin

object KubuszokRootPlugin extends AutoPlugin {

  override def trigger = noTrigger
  override def requires = KubuszokPlugin && WelcomePlugin
}
