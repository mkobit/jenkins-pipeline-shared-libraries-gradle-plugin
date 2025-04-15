package com.mkobit.jenkins.pipelines

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestTemplate
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.output
import strikt.gradle.testkit.task
import testsupport.junit.ForGradleVersions
import testsupport.junit.GradleProject
import testsupport.testkit.runner.build
import testsupport.testkit.runner.info

@ForGradleVersions
internal class UnitTestSourceIntegrationTest {

  @TestTemplate
  internal fun `can write unit tests using JenkinsPipelineUnit`(
    @GradleProject(["projects", "jenkins-pipeline-unit"]) gradleRunner: GradleRunner
  ) {
    val buildResult: BuildResult =
      gradleRunner
        .apply {
          info = true
        }.build("test")

    expectThat(buildResult)
      .task(":test")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `integrationTest task is not executed when test is executed`(
    @GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner
  ) {
    val buildResult: BuildResult =
      gradleRunner
        .apply {
          info = true
        }.build("test")

    expectThat(buildResult)
      .task(":integrationTest")
      .isNull()
  }

  @Disabled(
    "@Grab does not seem to be working with tests in Gradle. See https://stackoverflow.com/questions/16471096/any-alternative-to-grabconfig and https://stackoverflow.com/questions/4611230/no-suitable-classloader-found-for-grab",
  )
  @TestTemplate
  internal fun `can test @Grab using source can be unit tested normally`(
    @GradleProject(["projects", "source-with-@grab"]) gradleRunner: GradleRunner
  ) {
    val buildResult = gradleRunner.build("test", "--tests", "*GrabUsingLibraryTest*")

    expectThat(buildResult)
      .task(":test")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `can run tests that do not use the source code used by @Grab`(
    @GradleProject(["projects", "source-with-@grab"]) gradleRunner: GradleRunner
  ) {
    val buildResult = gradleRunner.build("test", "--tests", "*NonGrabUsingTest*")

    expectThat(buildResult)
      .task(":test")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `can test library code that makes use of Jenkins core and plugin classes`(
    @GradleProject(["projects", "global-library-using-jenkins-plugin-classes"]) gradleRunner: GradleRunner
  ) {
    val buildResult = gradleRunner.build("test")

    expectThat(buildResult)
      .output
      .contains("com.mkobit.LibraryUsingJenkinsClassesTest > throws exception for null constructor STARTED")
  }
}
