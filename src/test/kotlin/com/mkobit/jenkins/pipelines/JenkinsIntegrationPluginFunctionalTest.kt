package com.mkobit.jenkins.pipelines

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.TestTemplate
import testsupport.ForGradleVersions
import testsupport.GradleProject
import testsupport.NotImplementedYet

@ForGradleVersions
internal class JenkinsIntegrationPluginFunctionalTest {
  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the GDSL`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the global security whitelist`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insignificant privileges to retrieve the global security whitelist`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the list of plugins`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insignificant privileges to retrieve the list of plugins`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the Jenkins version`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the list of global library configurations`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insignificant privileges to retrieve the global library configurations`(@GradleProject gradleRunner: GradleRunner) {
  }
}
