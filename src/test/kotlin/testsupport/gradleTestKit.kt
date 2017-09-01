package testsupport

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

fun build(projectDir: File, vararg args: String): BuildResult = GradleRunner.create()
  .withPluginClasspath()
  .withArguments(*args)
  .withProjectDir(projectDir)
  .build()

fun GradleRunner.buildWithPluginClasspath(vararg args: String): BuildResult = withPluginClasspath().withArguments(*args).build()

fun GradleRunner.buildAndFailWithPluginClasspath(vararg args: String): BuildResult = withPluginClasspath().withArguments(*args).buildAndFail()
