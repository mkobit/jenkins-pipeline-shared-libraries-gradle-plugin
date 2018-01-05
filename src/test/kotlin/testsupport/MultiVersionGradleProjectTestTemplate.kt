package testsupport

import org.gradle.util.GradleVersion
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import java.util.logging.Logger
import java.util.stream.Stream

@Integration
@ExtendWith(MultiVersionGradleProjectTestTemplate::class)
@Target(AnnotationTarget.CLASS)
annotation class ForGradleVersions(
  val versions: Array<String> = arrayOf()
)

internal class MultiVersionGradleProjectTestTemplate : TestTemplateInvocationContextProvider {

  companion object {
    private val LOGGER: Logger = Logger.getLogger(MultiVersionGradleProjectTestTemplate::class.qualifiedName)
    // System property key to run tests for specified versions.
    // Versions can be specified as 'all', 'default', or semicolon separated (';') versions
    private val VERSIONS_PROPERTY_KEY: String = "${ForGradleVersions::class.qualifiedName!!}.versions"
    private val CURRENT_GRADLE_VERSION: GradleVersion by lazy {
      GradleVersion.current()
    }
    private val DEFAULT_VERSIONS: List<GradleVersion> by lazy {
      listOf(
        GradleVersion.version("4.3"),
        GradleVersion.version("4.4"),
        CURRENT_GRADLE_VERSION
      )
    }
  }

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    return context.testClass
      .flatMap { clazz -> AnnotationSupport.findAnnotation(clazz, ForGradleVersions::class.java) }
      .map { gradleVersions -> gradleVersions.versions }
      .map { versions -> versions.map(GradleVersion::version) }
      .map { gradleVersions -> determineVersionsToExecute(gradleVersions) }
      .map { gradleVersions -> gradleVersions.map(::GradleProjectInvocationContext) }
      .map { it.stream().distinct() }
      .orElseThrow { Exception("Don't think this should happen, as default values should be found") }
      .map { it as TestTemplateInvocationContext } // Needed for https://github.com/junit-team/junit5/issues/1226
  }

  /**
   * Determines the final versions of Gradle to execute a test with based on the:
   * 1. Annotations
   * 2. System properties
   * 3. Default versions at [DEFAULT_VERSIONS]
   */
  private fun determineVersionsToExecute(gradleVersions: Collection<GradleVersion>): Collection<GradleVersion> {
    val versionsFromSystemProperty: List<GradleVersion> by lazy {
      System.getProperty(VERSIONS_PROPERTY_KEY)?.let { value ->
        return@let when (value) {
          "all", "default" -> {
            LOGGER.info { "System property supplied default $value to use all/default versions $DEFAULT_VERSIONS" }
            DEFAULT_VERSIONS
          }
          "current" -> {
            LOGGER.info { "System property supplied $value so running only against current version $CURRENT_GRADLE_VERSION" }
            listOf(CURRENT_GRADLE_VERSION)
          }
          else -> {
            LOGGER.fine { "Determining versions from system property value $value" }
            value.split(";")
              .map(GradleVersion::version)
          }
        }
      } ?: emptyList()
    }

    return when {
      gradleVersions.isNotEmpty() -> versionsFromSystemProperty.intersect(gradleVersions).also {
        if (it.isEmpty()) {
          LOGGER.info { "No intersection between annotated versions and system property versions" }
        }
      }
      versionsFromSystemProperty.isNotEmpty() -> {
        LOGGER.info { "Using System property provided versions $versionsFromSystemProperty" }
        versionsFromSystemProperty
      }
      else -> {
        LOGGER.info { "No versions specified in code or by system properties so using default $DEFAULT_VERSIONS" }
        DEFAULT_VERSIONS
      }
    }
  }

  // better handle @NotImplemented cases
  override fun supportsTestTemplate(context: ExtensionContext): Boolean = context.testClass
      .map { clazz ->  AnnotationSupport.findAnnotation(clazz, ForGradleVersions::class.java) }
      .isPresent
}

data class GradleProjectInvocationContext(
  val version: GradleVersion
) : TestTemplateInvocationContext {

  override fun getDisplayName(invocationIndex: Int): String = "Gradle version: " + if (version == GradleVersion.current()) {
    "(compiled with) ${version.version}"
  } else {
    version.version
  }

  override fun getAdditionalExtensions(): List<Extension> = listOf(ResourceGradleProjectProviderExtension(version))
}
