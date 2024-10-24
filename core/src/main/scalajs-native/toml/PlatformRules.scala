package toml

import fastparse._

private[toml] trait PlatformRules {
  def date[$: P] = P(CharIn()).map(_ => null.asInstanceOf[Value])
}
