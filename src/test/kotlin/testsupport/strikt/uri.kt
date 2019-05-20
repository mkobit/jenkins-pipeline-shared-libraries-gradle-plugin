package testsupport.strikt

import strikt.api.Assertion
import java.net.URI

val <T : URI> Assertion.Builder<T>.authority
  get() = get { authority }

val <T : URI> Assertion.Builder<T>.port
  get() = get { port }

val <T : URI> Assertion.Builder<T>.scheme
  get() = get { scheme }
