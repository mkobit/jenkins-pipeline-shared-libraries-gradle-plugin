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
annotation class MultiGradleVersion(
  val versions: Array<String> = arrayOf()
)

internal class MultiVersionGradleProjectTestTemplate : TestTemplateInvocationContextProvider {

  companion object {
    private val currentGradleVersion: GradleVersion by lazy {
      GradleVersion.current()
    }
    private val DEFAULT_VERSIONS: List<GradleProjectInvocationContext> by lazy {
      listOf(
        GradleProjectInvocationContext(GradleVersion.version("4.3")),
        GradleProjectInvocationContext(GradleVersion.version("4.4")),
        GradleProjectInvocationContext(currentGradleVersion, "${currentGradleVersion.version} (current)")
      )
    }

  }

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    return context.testClass
      .flatMap { clazz -> AnnotationSupport.findAnnotation(clazz, MultiGradleVersion::class.java) }
      .map { gradleVersions -> gradleVersions.versions }
      .map { versions -> versions.map { GradleProjectInvocationContext(GradleVersion.version(it)) } }
      .map { contexts -> contexts.orDefault() }
      .map { it.toSet() }
      .map { it.stream() }
      .orElseThrow { Exception("Don't think this should happen") }
      .map { it as TestTemplateInvocationContext } // Why is this cast needed for Kotlin?
  }

  private fun List<GradleProjectInvocationContext>.orDefault(): List<GradleProjectInvocationContext> =
    if (isNotEmpty()) {
      this
    } else {
      DEFAULT_VERSIONS
    }

  // better handle @NotImplemented cases
  override fun supportsTestTemplate(context: ExtensionContext): Boolean = context.testClass
      .map { clazz ->  AnnotationSupport.findAnnotation(clazz, MultiGradleVersion::class.java) }
      .isPresent
}

private data class GradleProjectInvocationContext(
  val version: GradleVersion,
  private val overrideDisplayVersion: String? = null
) : TestTemplateInvocationContext {

  private val displayVersion: String
    get() = overrideDisplayVersion ?: version.version

  // TODO: improve this
  override fun getDisplayName(invocationIndex: Int): String = "Gradle version: $displayVersion"

  override fun getAdditionalExtensions(): List<Extension> = listOf(ResourceGradleProjectProviderExtension(version))
}
