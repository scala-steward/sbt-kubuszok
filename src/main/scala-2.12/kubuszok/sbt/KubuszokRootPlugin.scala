package kubuszok.sbt

import sbt.*
import sbtwelcome.WelcomePlugin

object KubuszokRootPlugin extends AutoPlugin {

  override def trigger = noTrigger
  override def requires = KubuszokPlugin && WelcomePlugin
}
