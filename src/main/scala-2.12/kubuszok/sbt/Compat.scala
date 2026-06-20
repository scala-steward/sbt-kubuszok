package kubuszok.sbt

// sbt-1.x variant: sbt 1.x has no task/setting result caching, so there is
// nothing to opt out of - `uncached` is the identity.
private[sbt] object Compat {

  def uncached[A](a: A): A = a
}
