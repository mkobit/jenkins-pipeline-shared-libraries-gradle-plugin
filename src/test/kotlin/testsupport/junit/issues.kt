package testsupport.junit

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport

@ExtendWith(IssueExtension::class)
@Target(AnnotationTarget.FUNCTION)
annotation class Issue(val url: String)

private class IssueExtension : BeforeTestExecutionCallback {
  override fun beforeTestExecution(context: ExtensionContext) {
    AnnotationSupport.findAnnotation(context.testMethod, Issue::class.java)
      .map { it.url }
      .ifPresent { context.publishReportEntry("issue", it) }
  }
}
