package testsupport.strikt

import org.gradle.api.provider.Property
import strikt.api.Assertion

fun <T, P : Property<T>> Assertion.Builder<P>.isPresent() =
  assert("is present") { it.isPresent }

val <T, P : Property<T>> Assertion.Builder<P>.value
  get() = get { get() }
