package testsupport

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files

internal class TestProjectBuilder : AutoCloseable {
  val dir: File = Files.createTempDirectory("shared-library-functional-test").toFile()
  val settingsFile: File = dir.resolve("settings.gradle.kts")
  val buildFile: File = dir.resolve("build.gradle.kts")

  init {
    settingsFile.writeText("""rootProject.name = "test-project"""")
  }

  fun file(path: String): File = dir.resolve(path).also { it.parentFile.mkdirs() }

  fun runner(gradleVersion: TestedGradleVersion): GradleRunner =
    GradleRunner
      .create()
      .withProjectDir(dir)
      .withGradleVersion(gradleVersion.version)
      .withPluginClasspath()

  override fun close() {
    dir.deleteRecursively()
  }
}
