package testsupport.minutest

import dev.minutest.TestContextBuilder
import dev.minutest.junit.testFactoryFor
import dev.minutest.rootContext
import org.junit.jupiter.api.DynamicNode
import java.util.stream.Stream

inline fun <reified T> testFactory(noinline builder: TestContextBuilder<Unit, T>.() -> Unit): Stream<out DynamicNode> =
  testFactoryFor(
    rootContext(builder = builder)
  )
