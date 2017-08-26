package testsupport

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

fun build(projectDir: File, vararg args: String): BuildResult = GradleRunner.create()
  .withPluginClasspath()
  .withArguments(*args)
  .withProjectDir(projectDir)
  .build()
