package testsupport

import org.assertj.core.api.SoftAssertions

fun softlyAssert(assertions: SoftAssertions.() -> Unit) {
  val softAssertions = SoftAssertions()
  softAssertions.assertions()
  softAssertions.assertAll()
}
