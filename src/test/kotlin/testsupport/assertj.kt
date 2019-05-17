package testsupport

import org.assertj.core.api.Assertions.assertThatCode

fun expectDoesNotThrow(body: () -> Unit) = assertThatCode(body)
  .doesNotThrowAnyException()
