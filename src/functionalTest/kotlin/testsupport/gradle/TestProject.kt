package testsupport.gradle

import io.kotest.core.TestConfiguration
import io.kotest.engine.spec.tempdir
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import kotlin.io.path.createDirectories

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

context(config: TestConfiguration)
fun withTestProject(block: TestProject.() -> Unit) {
  val project = TestProject(config.tempdir("shared-library-functional-test").toPath())
  project.block()
}
