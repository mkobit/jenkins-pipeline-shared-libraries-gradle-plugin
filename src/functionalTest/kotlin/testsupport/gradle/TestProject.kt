package testsupport.gradle

import io.kotest.core.TestConfiguration
import io.kotest.engine.spec.tempdir
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
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
        System.getProperty("test.gradle.user.home")?.let { withTestKitDir(Path(it).toFile()) }
      }
}

data class GradleProperties(
  val jvmArgs: String? = "-Xmx512m -XX:MaxMetaspaceSize=384m",
) {
  fun writeTo(path: Path) {
    val props = java.util.Properties()
    jvmArgs?.let { props.setProperty("org.gradle.jvmargs", it) }

    if (props.isNotEmpty()) {
      path.outputStream().use { os ->
        props.store(os, null)
      }
    }
  }
}

context(config: TestConfiguration)
fun withTestProject(
  // Without an explicit -Xmx the daemon JVM uses ergonomic defaults (25% of system RAM).
  gradleProperties: GradleProperties = GradleProperties(),
  block: TestProject.() -> Unit,
) {
  val dir = config.tempdir("shared-library-functional-test").toPath()
  gradleProperties.writeTo(dir.resolve("gradle.properties"))
  TestProject(dir).block()
}
