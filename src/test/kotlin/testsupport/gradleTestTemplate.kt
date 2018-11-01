package testsupport

import com.google.common.io.Resources
import com.mkobit.gradle.test.kotlin.testkit.runner.projectDirPath
import com.mkobit.gradle.test.kotlin.testkit.runner.stacktrace
import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
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
annotation class GradleProject(val resourcePath: Array<String> = [])

@Integration
@ExtendWith(MultiVersionGradleProjectTestTemplate::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ForGradleVersions(
  val versions: Array<String> = []
)

internal class MultiVersionGradleProjectTestTemplate : TestTemplateInvocationContextProvider {
  companion object {
    private val DEFAULT_VERSIONS: Set<GradleVersion> by lazy {
      setOf(
        GradleVersion.current(),
        GradleVersion.version("5.0-rc-1")
      )
    }
  }

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    // Prioritize method annotation over class annotation
    return findGradleVersions(context)
      .map { gradleVersions -> gradleVersions.versions }
      .map { versions ->
        when {
          // Handle case where version is specified as 'current'
          versions.size == 1 && versions.first().toLowerCase() == "current" -> listOf(GradleVersion.current())
          versions.isNotEmpty() -> versions.map(GradleVersion::version)
          else ->  DEFAULT_VERSIONS
        }
      }
      .map { gradleVersions -> gradleVersions.map { GradleProjectInvocationContext(context.displayName, it) } }
      .map {
        it.stream().distinct()
          .map {
            // Needed because of https://github.com/junit-team/junit5/issues/1226
            it as TestTemplateInvocationContext
          }
      }
      .orElseThrow { RuntimeException("Could not create invocation contexts") }
  }

  override fun supportsTestTemplate(context: ExtensionContext): Boolean = findGradleVersions(context).isPresent

  private fun findGradleVersions(context: ExtensionContext): Optional<ForGradleVersions> =
    AnnotationSupport.findAnnotation(context.testMethod, ForGradleVersions::class.java)
      .or { AnnotationSupport.findAnnotation(context.testClass, ForGradleVersions::class.java) }
}

private data class GradleProjectInvocationContext(
  private val contextDisplay: String,
  private val version: GradleVersion
) : TestTemplateInvocationContext {

  override fun getDisplayName(invocationIndex: Int): String  = "[Gradle " +
    if (version.equals(GradleVersion.current())) {
      "${version.version} (current)"
    } else {
      version.version
    } + "] ⇒ $contextDisplay"

  override fun getAdditionalExtensions(): List<Extension> = listOf(
    ResourceGradleProjectProviderExtension(version),
    FilteringGradleExecutionCondition(version)
  )
}

private class FilteringGradleExecutionCondition(
  private val targetVersion: GradleVersion
) : ExecutionCondition {

  companion object {
    // System property key to run tests for specified versions.
    // Versions can be specified as 'all', 'default', or semicolon separated (';') versions
    private val VERSIONS_PROPERTY_KEY: String = "${ForGradleVersions::class.qualifiedName!!}.versions"
    private val DEFAULT_CONDITION: ConditionEvaluationResult =
      ConditionEvaluationResult.enabled("No filter specified")
  }

  override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
    val propertyValue = System.getProperty(VERSIONS_PROPERTY_KEY) ?: return DEFAULT_CONDITION

    return when (propertyValue) {
      "all", "default" -> ConditionEvaluationResult.enabled("All tests enabled through $propertyValue filter")
      "current" -> {
        if (targetVersion.equals(GradleVersion.current())) {
          ConditionEvaluationResult.enabled("Target version is the current version")
        } else {
          ConditionEvaluationResult.disabled("Target version $targetVersion is not the current version ${GradleVersion.current()}")
        }
      }
      else -> {
        val systemPropertyVersions = propertyValue.split(";").map(GradleVersion::version)
        if (targetVersion in systemPropertyVersions) {
          ConditionEvaluationResult.enabled("Target version $targetVersion is in System property specified versions $systemPropertyVersions")
        } else {
          ConditionEvaluationResult.disabled("Target version $targetVersion is not in System property specified versions $systemPropertyVersions")
        }
      }
    }
  }
}

private class ResourceGradleProjectProviderExtension(
  private val gradleVersion: GradleVersion
) : ParameterResolver, AfterTestExecutionCallback {

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
    val temporaryPath: Path = loadGradleProject(parameterContext, context)
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

  private fun loadGradleProject(parameterContext: ParameterContext, context: ExtensionContext): Path {
    val rootResourceDirectoryName = AnnotationSupport.findAnnotation(parameterContext.parameter, GradleProject::class.java)
      .map(GradleProject::resourcePath)
      .flatMap {
        when {
          it.isNotEmpty() -> Optional.of(it.joinToString(File.separator))
          else -> Optional.empty()
        }
      }.orElseGet {
        val testMethod: Method = context.requiredTestMethod
        val resourcePathToMethod = testMethod.kotlinFunction!!.name
        val testClass: Class<*> = context.requiredTestClass
        val resourcePathToClass = testClass.kotlin.qualifiedName!!.replace(".", "/")
        "$resourcePathToClass${File.separator}$resourcePathToMethod"
      }
    LOGGER.debug { "Loading Gradle project resources from classpath at $rootResourceDirectoryName for test ${context.uniqueId}" }
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
