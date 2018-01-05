package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.kotlin.testkit.runner.arguments
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import testsupport.ForGradleVersions
import testsupport.GradleProject
import testsupport.NotImplementedYet

@ForGradleVersions
class MainSourceIntegrationTest {

  @TestTemplate
  internal fun `main Groovy code is compiled`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.run {
      arguments("compileGroovy")
      build()
    }

    val task = buildResult.task(":compileGroovy")
    assertThat(task?.outcome)
      .describedAs("compileGroovy task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @TestTemplate
  internal fun `can unit test code in src`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.run {
      arguments("test")
      build()
    }

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

  @TestTemplate
  internal fun `Groovydoc JAR can be generated`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.run {
      arguments("groovydocJar")
      build()
    }

    val task = buildResult.task(":groovydocJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @TestTemplate
  internal fun `Groovy sources JAR can be generated`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.run {
      arguments("sourcesJar")
      build()
    }

    val task = buildResult.task(":sourcesJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }
}
