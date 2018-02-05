package testsupport

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.logging.Logger

@Disabled("test or feature not implemented yet")
annotation class NotImplementedYet

@Tag("integration")
@ExtendWith(TestExecutionLogger::class)
internal annotation class Integration

@Tag("intellij-support")
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION
)
annotation class IntelliJSupport

class TestExecutionLogger : BeforeTestExecutionCallback, AfterTestExecutionCallback {

  companion object {
    // java.util.Logger used for JUnit
    private val LOGGER = Logger.getLogger(TestExecutionLogger::class.qualifiedName)
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    LOGGER.info { "Before test: ${context.uniqueId}" }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    LOGGER.info { "After test: ${context.uniqueId}" }
  }
}
