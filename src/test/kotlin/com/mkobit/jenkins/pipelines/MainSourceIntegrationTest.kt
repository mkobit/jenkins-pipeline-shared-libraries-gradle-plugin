package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.assertj.GradleAssertions.assertThat
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.buildAndFail
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
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

  @TestTemplate
  internal fun `@Grab can be used in source code compilation`(@GradleProject(["projects", "source-with-@grab"]) gradleRunner: GradleRunner) {
    gradleRunner.build("compileGroovy")
  }

  @TestTemplate
  internal fun `Groovydoc JAR can be generated`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("groovydocJar")

    assertThat(buildResult)
      .hasTaskSuccessAtPath(":groovydocJar")
  }

  @TestTemplate
  internal fun `Groovy sources JAR can be generated`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("sourcesJar")

    assertThat(buildResult)
      .hasTaskSuccessAtPath(":sourcesJar")
  }
}
