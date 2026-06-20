package kubuszok.sbt

import sbt.Def

// sbt 2.0 variant: sbt 2.0 caches task/setting results and requires every
// captured value to have a sjsonnew.HashWriter. For values that intentionally
// should not be cached (custom enums, UpdateReport-derived results, ...), opt
// out via Def.uncached.
private[sbt] object Compat {

  inline def uncached[A](inline a: A): A = Def.uncached(a)
}
