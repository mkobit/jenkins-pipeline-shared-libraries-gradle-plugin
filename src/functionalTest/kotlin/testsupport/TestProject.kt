package testsupport

import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class TestProject(
  val dir: Path,
) {
  val settingsFile: Path = dir.resolve("settings.gradle.kts")
  val buildFile: Path = dir.resolve("build.gradle.kts")

  fun file(path: String): Path = dir.resolve(path).also { it.parent.createDirectories() }

  fun runner(gradleVersion: TestedGradleVersion): GradleRunner =
    GradleRunner
      .create()
      .withProjectDir(dir.toFile())
      .withGradleVersion(gradleVersion.version)
      .withPluginClasspath()
      .apply {
        System.getProperty("test.gradle.user.home")?.let { withTestKitDir(Path.of(it).toFile()) }
      }
}

inline fun withTestProject(block: TestProject.() -> Unit) {
  val dir = createTempDirectory("shared-library-functional-test")
  val project = TestProject(dir)
  project.settingsFile.writeText("""rootProject.name = "test-project"""")
  try {
    project.block()
  } finally {
    dir.toFile().deleteRecursively()
  }
}
