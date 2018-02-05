package testsupport

import com.google.common.io.Resources
import com.mkobit.gradle.test.kotlin.testkit.runner.projectDirPath
import com.mkobit.gradle.test.kotlin.testkit.runner.stacktrace
import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import java.io.File
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.Optional
import java.util.logging.Logger
import java.util.stream.Stream
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class GradleProject

@Integration
@ExtendWith(MultiVersionGradleProjectTestTemplate::class)
@Target(AnnotationTarget.CLASS)
annotation class ForGradleVersions(
  val versions: Array<String> = []
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
    private val DEFAULT_VERSIONS: Set<GradleVersion> by lazy {
      setOf(
        GradleVersion.version("4.3"),
        GradleVersion.version("4.4"),
        GradleVersion.version("4.5"),
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
      .map { it as TestTemplateInvocationContext } // Needed because of https://github.com/junit-team/junit5/issues/1226
  }

  /**
   * Determines the final versions of Gradle to execute a test with based on the:
   * 1. Annotations
   * 2. System properties
   * 3. Default versions at [DEFAULT_VERSIONS]
   */
  private fun determineVersionsToExecute(gradleVersions: Collection<GradleVersion>): Collection<GradleVersion> {
    val versionsFromSystemProperty: Collection<GradleVersion> by lazy {
      System.getProperty(VERSIONS_PROPERTY_KEY)?.let { value ->
        return@let when (value) {
          "all", "default" -> {
            LOGGER.fine { "System property supplied default $value to use all/default versions $DEFAULT_VERSIONS" }
            DEFAULT_VERSIONS
          }
          "current" -> {
            LOGGER.fine { "System property supplied $value so running only against current version $CURRENT_GRADLE_VERSION" }
            listOf(CURRENT_GRADLE_VERSION)
          }
          else -> {
            LOGGER.fine { "Determining versions from system property value $value" }
            value.split(";")
              .map(GradleVersion::version)
          }
        }
      } ?: emptySet()
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

private class ResourceGradleProjectProviderExtension(private val gradleVersion: GradleVersion) : ParameterResolver, AfterTestExecutionCallback {

  companion object {
    private val LOGGER = KotlinLogging.logger { }
  }

  override fun supportsParameter(
    parameterContext: ParameterContext,
    extensionContext: ExtensionContext
  ): Boolean {
    return parameterContext.parameter.type == GradleRunner::class.java
      && parameterContext.parameter.isAnnotationPresent(GradleProject::class.java)
  }

  override fun resolveParameter(
    parameterContext: ParameterContext,
    context: ExtensionContext
  ): Any {
    val executable = parameterContext.declaringExecutable
    if (executable is Constructor<*>) {
      throw IllegalArgumentException("Cannot resolve parameter for constructor $executable")
    }
    val store = getStore(context)
    val temporaryPath: Path = loadGradleProject(context)
    store.put(context, temporaryPath)
    return GradleRunner.create().apply {
      projectDirPath = temporaryPath
      stacktrace = true
      withGradleVersion(gradleVersion.version)
      withPluginClasspath()
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    val store = getStore(context)
    val temporaryPath: Path? = store.get(context, Path::class.java)
    LOGGER.debug { "Cleaning up directory at $temporaryPath" }
    temporaryPath?.let {
      Files.walkFileTree(it, RecursiveDeleteVisitor())
    }
  }

  private fun getStore(context: ExtensionContext): ExtensionContext.Store {
    return context.getStore(ExtensionContext.Namespace.create(
      ResourceGradleProjectProviderExtension::class.java, context))
  }

  private fun loadGradleProject(context: ExtensionContext): Path {
    val rootResourceDirectoryName = context.let {
      val testMethod: Method = context.requiredTestMethod
      val resourcePathToMethod = testMethod.kotlinFunction!!.name
      val testClass: Class<*> = context.requiredTestClass
      val resourcePathToClass = testClass.kotlin.qualifiedName!!.replace(".", "/")
      "$resourcePathToClass${File.separator}$resourcePathToMethod"
    }
    LOGGER.debug { "Loading Gradle project resources from classpath at $rootResourceDirectoryName" }
    return Resources.getResource(rootResourceDirectoryName).let {
      // This probably won't work for JAR or other protocols, so don't even try
      if (it.protocol != "file") {
        throw ParameterResolutionException("Resource at $it is not a file protocol")
      }
      val resourcesPath = File(it.toURI()).toPath()
      if (!Files.isDirectory(resourcesPath)) {
        throw ParameterResolutionException("Resource at $resourcesPath is not a directory")
      }
      val temporaryDirectory = createTempDirectory(context)
      LOGGER.debug { "Creating temporary directory for project at $temporaryDirectory" }
      Files.walkFileTree(resourcesPath, RecursiveCopyVisitor(resourcesPath, temporaryDirectory))
      temporaryDirectory
    }
  }

  private fun createTempDirectory(context: ExtensionContext): Path {
    fun sanitizePrefix(name: String): String = name.replace(" ", "_")

    val tempDirPrefix: String = context.testMethod
      .map { it.name }
      .or { context.testClass.map { it.name } }
      .or { Optional.of(context.displayName) }
      .map { sanitizePrefix(it) }
      .orElseGet { "default_prefix" }

    return Files.createTempDirectory(tempDirPrefix)
  }

  private class RecursiveCopyVisitor(val from: Path, val to: Path) : SimpleFileVisitor<Path>() {
    init {
      require(Files.isDirectory(from)) { "$from must be a directory" }
      require(Files.isDirectory(to)) { "$to must be a directory" }
    }

    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
      val targetPath = to.resolve(from.relativize(dir))
      if (!Files.exists(targetPath)) {
        LOGGER.debug { "Creating directory at $targetPath" }
        Files.createDirectory(targetPath)
      }
      return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
      val targetPath = to.resolve(from.relativize(file))
      LOGGER.debug { "Copying file from $to to $targetPath" }
      Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING)
      return FileVisitResult.CONTINUE
    }
  }

  private class RecursiveDeleteVisitor : SimpleFileVisitor<Path>() {
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
      LOGGER.debug { "Deleting file $file" }
      Files.delete(file)
      return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
      if (exc == null) {
        LOGGER.debug { "Deleting directory $dir" }
        Files.delete(dir)
        return FileVisitResult.CONTINUE
      }
      throw exc
    }
  }
}

private fun <T> Optional<T>.or(supplier: () -> Optional<T>): Optional<T> = if (isPresent) {
  this
} else {
  supplier()
}
