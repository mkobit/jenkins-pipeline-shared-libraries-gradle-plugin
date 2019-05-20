package testsupport.junit

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag

@Disabled("test or feature not implemented yet")
annotation class NotImplementedYet

@Tag("integration")
internal annotation class Integration

@Tag("intellij-support")
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.FUNCTION
)
annotation class IntelliJSupport
