package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.assertj.GradleAssertions.assertThat
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.buildAndFail
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
  internal fun `main Groovy code is compiled`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("compileGroovy")

    assertThat(buildResult)
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskSuccessAtPath(":compileGroovy")
  }

  @TestTemplate
  internal fun `can unit test code in src`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("test")

    assertThat(buildResult)
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskSuccessAtPath(":test")
  }

  @TestTemplate
  internal fun `compilation fails for invalid Groovy code in src`(@GradleProject(["projects", "invalid-src-groovy"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildAndFail("compileGroovy")

    assertThat(buildResult)
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskFailedAtPath(":compileGroovy")
  }

  @TestTemplate
  internal fun `compilation fails for invalid Groovy code in vars`(@GradleProject(["projects", "invalid-vars-groovy"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildAndFail("compileGroovy")

    assertThat(buildResult)
      .hasTaskFailedAtPath(":compileGroovy")
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
  internal fun `Groovydoc JAR can be generated`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("groovydocJar")

    val task = buildResult.task(":groovydocJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @TestTemplate
  internal fun `Groovy sources JAR can be generated`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("sourcesJar")

    val task = buildResult.task(":sourcesJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }
}
