package testsupport.gradle

import io.kotest.core.TestConfiguration
import io.kotest.engine.spec.tempdir
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class TestProject(
  val dir: Path,
) {
  val settingsFile: Path = dir.resolve("settings.gradle.kts")
  val buildFile: Path = dir.resolve("build.gradle.kts")

  // Without an explicit -Xmx the daemon JVM uses ergonomic defaults (25% of system RAM).
  private val gradleProperties: MutableMap<String, String> =
    mutableMapOf("org.gradle.jvmargs" to "-Xmx512m -XX:MaxMetaspaceSize=384m")
  private var gradlePropertiesFlushed = false

  fun file(path: String): Path = dir.resolve(path).also { it.parent.createDirectories() }

  fun gradleProperty(
    key: String,
    value: String,
  ): TestProject {
    check(!gradlePropertiesFlushed) { "gradleProperty() called after runner() — properties already written to disk" }
    gradleProperties[key] = value
    return this
  }

  fun runner(gradleVersion: TestedGradleVersion): GradleRunner {
    if (!gradlePropertiesFlushed) {
      dir.resolve("gradle.properties").writeText(
        gradleProperties.entries.joinToString("\n") { (k, v) -> "$k=$v" } + "\n",
      )
      gradlePropertiesFlushed = true
    }
    return GradleRunner
      .create()
      .withProjectDir(dir.toFile())
      .withGradleVersion(gradleVersion.version)
      .withPluginClasspath()
      .apply {
        System.getProperty("test.gradle.user.home")?.let { withTestKitDir(Path.of(it).toFile()) }
      }
  }
}

context(config: TestConfiguration)
fun withTestProject(block: TestProject.() -> Unit) {
  TestProject(config.tempdir("shared-library-functional-test").toPath()).block()
}
