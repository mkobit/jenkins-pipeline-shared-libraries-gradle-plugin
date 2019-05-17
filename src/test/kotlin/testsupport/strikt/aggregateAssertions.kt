package testsupport.strikt

import strikt.api.Assertion

// core built-in request at https://github.com/robfletcher/strikt/issues/165

fun <T> Assertion.Builder<T>.allOf(assertions: Assertion.Builder<T>.(T) -> Unit) =
  compose("all of") {
    assertions(it)
  } then {
    when {
      allPassed -> pass()
      else -> fail()
    }
  }

fun <T> Assertion.Builder<T>.anyOf(assertions: Assertion.Builder<T>.(T) -> Unit) =
  compose("any of") {
    assertions(it)
  } then {
    when {
      anyPassed -> pass()
      else -> fail()
    }
  }
