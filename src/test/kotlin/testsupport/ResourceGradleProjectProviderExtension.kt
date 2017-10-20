package testsupport

import com.google.common.io.Resources
import mu.KotlinLogging
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.ParameterResolver
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
import kotlin.reflect.jvm.kotlinFunction

internal class ResourceGradleProjectProviderExtension(private val gradleVersion: String?) : ParameterResolver, AfterTestExecutionCallback {

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
    val runner = GradleRunner.create()
    val store = getStore(context)
    val temporaryPath: Path = loadGradleProject(context)
    store.put(context, temporaryPath)
    runner.withProjectDir(temporaryPath.toFile())
    if (gradleVersion != null) {
      runner.withGradleVersion(gradleVersion)
    }

    return runner
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
      val testMethod: Method = context.testMethod.orElseThrow { ParameterResolutionException("No test method") }
      val resourcePathToMethod = testMethod.kotlinFunction!!.name
      val testClass: Class<*> = context.testClass.orElseThrow { ParameterResolutionException("No test class") }
      val resourcePathToClass = testClass.kotlin.qualifiedName!!.replace(".", "/")
      "$resourcePathToClass/$resourcePathToMethod"
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
    val tempDirName: String = when {
      context.testMethod.isPresent -> context.testMethod.get().name
      context.testClass.isPresent -> context.testClass.get().name
      else -> context.displayName
    }

    return Files.createTempDirectory(tempDirName)
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
