package testsupport

import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import java.util.function.Predicate

/**
 * Kotlin friendly soft assertions.
 */
fun softlyAssert(assertions: SoftAssertions.() -> Unit) = SoftAssertions.assertSoftly(assertions)

/**
 * Kotlin friendly custom [Condition] types.
 */
fun <T> condition(description: String, predicate: T.() -> Boolean): Condition<T> = Condition(Predicate { predicate(it) }, description)
