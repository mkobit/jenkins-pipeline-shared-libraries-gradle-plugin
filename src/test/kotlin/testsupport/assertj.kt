package testsupport

import com.mkobit.gradle.test.assertj.GradleSoftAssertions
import com.mkobit.gradle.test.assertj.testkit.BuildResultAssert
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.gradle.testkit.runner.BuildResult
import java.util.function.Predicate

/**
 * Kotlin friendly soft assertions.
 */
fun softlyAssert(assertions: SoftAssertions.() -> Unit) = SoftAssertions.assertSoftly(assertions)

/**
 * Kotlin friendly soft assertions for [BuildResult].
 */
fun softlyAssert(buildResult: BuildResult, assertions: BuildResultAssert.() -> Unit) = GradleSoftAssertions().run {
  assertThat(buildResult).assertions()
  assertAll()
}

/**
 * Kotlin friendly custom [Condition] types.
 */
fun <T> condition(description: String, predicate: T.() -> Boolean): Condition<T> = Condition(Predicate { predicate(it) }, description)
