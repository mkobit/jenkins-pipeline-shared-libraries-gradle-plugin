package testsupport.kotest

import io.kotest.matchers.shouldBe
import org.gradle.api.provider.Provider

fun <T : Any> Provider<T>.shouldBePresent(): T {
  isPresent shouldBe true
  return get()
}

fun <T : Any> Provider<T>.shouldNotBePresent() {
  isPresent shouldBe false
}

infix fun <T : Any> Provider<T>.shouldHaveValue(expected: T) {
  isPresent shouldBe true
  get() shouldBe expected
}

fun <T : Any> Provider<T>.shouldBePresent(block: (T) -> Unit) {
  isPresent shouldBe true
  block(get())
}
