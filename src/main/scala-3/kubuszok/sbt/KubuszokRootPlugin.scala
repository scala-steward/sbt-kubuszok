package kubuszok.sbt

import sbt.*

// sbt 2.0 variant: sbt-welcome has no final sbt-2.0 build, so the root plugin
// does not require WelcomePlugin here (it is only available on the sbt-1.x axis).
object KubuszokRootPlugin extends AutoPlugin {

  override def trigger = noTrigger
  override def requires = KubuszokPlugin
}
