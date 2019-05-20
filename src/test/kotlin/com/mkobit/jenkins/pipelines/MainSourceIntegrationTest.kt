package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.buildAndFail
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.gradle.testkit.isFailed
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.task
import testsupport.junit.ForGradleVersions
import testsupport.junit.GradleProject
import testsupport.junit.NotImplementedYet
import testsupport.assertj.expectDoesNotThrow

@ForGradleVersions
class MainSourceIntegrationTest {

  @TestTemplate
  internal fun `main Groovy code is compiled`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("compileGroovy")

    expectThat(buildResult)
      .task(":compileGroovy")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `can unit test code in src`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("test")

    expectThat(buildResult)
      .task(":test")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `compilation fails for invalid Groovy code in src`(@GradleProject(["projects", "invalid-src-groovy"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildAndFail("compileGroovy")

    expectThat(buildResult)
      .task(":compileGroovy")
      .isNotNull()
      .isFailed()
  }

  @TestTemplate
  internal fun `compilation fails for invalid Groovy code in vars`(@GradleProject(["projects", "invalid-vars-groovy"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildAndFail("compileGroovy")

    expectThat(buildResult)
      .task(":compileGroovy")
      .isNotNull()
      .isFailed()
  }

  @NotImplementedYet
  @Test
  internal fun `cannot add dependencies for compilation or execution`() {
  }

  @TestTemplate
  internal fun `@Grab can be used in source code compilation`(@GradleProject(["projects", "source-with-@grab"]) gradleRunner: GradleRunner) {
    expectDoesNotThrow {
      gradleRunner.build("compileGroovy")
    }
  }

  @TestTemplate
  internal fun `Groovydoc JAR can be generated`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("groovydocJar")

    expectThat(buildResult)
      .task(":groovydocJar")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `Groovy sources JAR can be generated`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("sourcesJar")

    expectThat(buildResult)
      .task(":sourcesJar")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `can use Jenkins core and plugin classes in main library code`(@GradleProject(["projects", "global-library-using-jenkins-plugin-classes"]) gradleRunner: GradleRunner) {
    expectDoesNotThrow {
      gradleRunner.build("compileGroovy")
    }
  }
}
