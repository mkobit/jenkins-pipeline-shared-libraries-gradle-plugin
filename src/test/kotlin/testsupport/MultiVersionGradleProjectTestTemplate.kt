package testsupport

import org.gradle.util.GradleVersion
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import java.util.stream.Stream

@ExtendWith(MultiVersionGradleProjectTestTemplate::class)
@Target(AnnotationTarget.CLASS)
annotation class GradleVersions(val versions: Array<String> = emptyArray())

internal class MultiVersionGradleProjectTestTemplate : TestTemplateInvocationContextProvider {
  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    return context.testClass
      .flatMap { clazz -> AnnotationSupport.findAnnotation(clazz, GradleVersions::class.java) }
      .map { gradleVersions -> gradleVersions.versions }
      .map { versions -> versions.map { GradleProjectInvocationContext.ForVersion(it) } }
      .map { contexts -> contexts + listOf(GradleProjectInvocationContext.CurrentVersion) }
      .map { it.toSet() }
      .map { it.stream() }
      .orElseThrow { Exception("Don't think this should happen") }
      .map { it as TestTemplateInvocationContext } // Why is this cast needed for Kotlin?
  }

  // better handle @NotImplemented cases
  override fun supportsTestTemplate(context: ExtensionContext): Boolean = context.testClass
      .map { clazz ->  AnnotationSupport.findAnnotation(clazz, GradleVersions::class.java) }
      .isPresent
}

private sealed class GradleProjectInvocationContext(
  private val displayVersion: String,
  private val version: String?
) : TestTemplateInvocationContext {

  // TODO: improve this
  override fun getDisplayName(invocationIndex: Int): String = "Gradle version: $displayVersion"

  override fun getAdditionalExtensions(): List<Extension> = listOf(ResourceGradleProjectProviderExtension(version))

  object CurrentVersion : GradleProjectInvocationContext("current [${GradleVersion.current().version}]", null)

  class ForVersion(version: String) : GradleProjectInvocationContext("[$version]", version)
}
