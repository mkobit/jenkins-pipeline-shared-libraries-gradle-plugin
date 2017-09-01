package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testsupport.GradleProject
import testsupport.Integration
import testsupport.NotImplementedYet
import testsupport.buildWithPluginClasspath
import java.io.File

@Integration
class MainSourceIntegrationTest {
  private lateinit var projectDir: File

  @BeforeEach
  internal fun setUp() {
    projectDir = createTempDir().apply { deleteOnExit() }
  }

  // TODO: test both groovy and kotlin usages
  @Test
  internal fun `main Groovy code is compiled`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWithPluginClasspath("compileGroovy")

    val task = buildResult.task(":compileGroovy")
    assertThat(task?.outcome)
      .describedAs("compileGroovy task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `can unit test code in src`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWithPluginClasspath("test", "-s")

    val task = buildResult.task(":test")
    assertThat(task?.outcome)
      .describedAs("test task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @NotImplementedYet
  @Test
  internal fun `@NonCPS can be used in source code`() {
  }

  @NotImplementedYet
  @Test
  internal fun `cannot add dependencies for compilation or execution`() {
  }

  @NotImplementedYet
  @Test
  internal fun `@Grab in library source is supported for trusted libraries`() {
  }

  @NotImplementedYet
  @Test
  internal fun `@Grab not supported for untrusted libraries`() {
  }

  @Test
  internal fun `Groovydoc JAR can be generated`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWithPluginClasspath("groovydocJar")

    val task = buildResult.task(":groovydocJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `Groovy sources JAR can be generated`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWithPluginClasspath("sourcesJar")

    val task = buildResult.task(":sourcesJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }
}
