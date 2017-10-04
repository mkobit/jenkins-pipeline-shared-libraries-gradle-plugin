package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.testkit.runner.buildWith
import org.assertj.core.api.Assertions.anyOf
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testsupport.GradleProject
import testsupport.Integration
import testsupport.IntelliJSupport
import testsupport.NotImplementedYet
import testsupport.SampleCandidate
import testsupport.condition
import testsupport.softlyAssert
import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments.of as arguments

@Integration
internal class IntegrationTestSourceIntegrationTest {

  @Disabled("may not be artifacts but file dependencies with current hack")
  @Test
  internal fun `Jenkins Pipeline Shared Groovy Libraries Plugin JAR available in integrationTestCompileClasspath configuration`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("printOutDependencies"))

    assertThat(buildResult.output).contains("Artifact: org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:jar")
  }

  @Disabled("may not be artifacts but file dependencies with current hack")
  @Test
  internal fun `Jenkins Pipeline Shared Groovy Libraries Plugin HPI available in integrationRuntimeClasspath configuration`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("printOutDependencies"))

    assertThat(buildResult.output).contains("Artifact: org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:hpi")
  }

  @Test
  internal fun `no HPI artifacts exist in jenkinsPluginLibraries configuration`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("--quiet", "showConfigurationFiles"))

    softlyAssert {
      assertThat(buildResult.output.trim().split(System.lineSeparator())).isNotEmpty.allSatisfy {
        assertThat(it).endsWith(".jar")
      }
    }
  }

  @Test
  internal fun `only HPI or JPI artifacts exist in jenkinsPluginHpisAndJpis configuration`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("--quiet", "showConfigurationFiles"))

    println(buildResult.output)
    softlyAssert {
      assertThat(buildResult.output.trim().split(System.lineSeparator())).isNotEmpty.allSatisfy {
        val hpi = condition<String>(".hpi extension") { it.endsWith(".hpi") }
        val jpi = condition<String>(".jpi extension") { it.endsWith(".jpi") }
        assertThat(it).has(anyOf(hpi, jpi))
      }
    }
  }

  @Test
  internal fun `can compile integration test sources that use Jenkins libraries`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf( "compileIntegrationTestGroovy", "-i"))

    val task = buildResult.task(":compileIntegrationTestGroovy")
    assertThat(task?.outcome)
      .describedAs("integrationTestCompileGroovy task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `can use @JenkinsRule in integration tests`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf( "integrationTest", "-i"))

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `WorkflowJob can be created and executed in integration tests`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("integrationTest", "-i"))

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `can set up Global Pipeline Library and use them in an integration test`(@GradleProject gradleRunner: GradleRunner) {
    Git.init().setDirectory(gradleRunner.projectDir).call().use {
      it.add().addFilepattern(".").call()
      it.commit().setMessage("Commit all the files").setAuthor("Mr. Manager", "mrmanager@example.com").call()
    }

    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf( "integrationTest", "-i"))

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @NotImplementedYet
  @Test
  @IntelliJSupport
  internal fun `pipeline resources have main sourceSet available on compile classpath for tooling assistance with IDEA`() {
  }

  @NotImplementedYet
  @Test
  @IntelliJSupport
  internal fun `can use pipeline resources in integration tests`() {
  }

  @NotImplementedYet
  @Test
  internal fun `no configurations are resolved during configuration phase`() {
  }

  @Test
  internal fun `Groovy DSL extension configuration`(@GradleProject gradleRunner: GradleRunner) {
    gradleRunner.buildWith(arguments = listOf("-i"))
  }

  @Disabled("This example works in a local Gradle project, but not with Gradle Test Kit")
  @Test
  internal fun `Kotlin DSL extension configuration`(@GradleProject gradleRunner: GradleRunner) {
    gradleRunner.buildWith(arguments = listOf("-i"))
  }

  @NotImplementedYet
  @ParameterizedTest(name = "'{1}'")
  @MethodSource("stepTests")
  internal fun `can use step`(stepName: String, stepBody: String) {
  }

  @Suppress("UNUSED")
  private fun stepTests(): Stream<Arguments> {
    return Stream.of(
      arguments("stage", "stage('example stage') {}"),
      arguments("sh", """sh('echo "hello"')"""),
      arguments("node", "node {}")
    )
  }

  @NotImplementedYet
  @Test
  internal fun `GlobalLibraries is available for configuration in integration tests`() {
  }

  @NotImplementedYet
  @Test
  internal fun `integration test output for Jenkins Test Harness is in the build directory`() {
  }

  // TODO: this should be tested but may or may not be needed. I have a feeling classloader errors
  // will happen in pipeline code if those classes are available.
  @NotImplementedYet
  @Test
  internal fun `cannot use classes from main source code in integration test`() {
  }

  @NotImplementedYet
  @Test
  internal fun `can use declared plugin dependencies in integration test`() {
  }

  // TODO: figure out better way to split out sample-like tests
  @NotImplementedYet
  @SampleCandidate
  @Test
  internal fun `can use parameterized pipeline build`() {
  }

  @NotImplementedYet
  @SampleCandidate
  @Test
  internal fun `can use Spock testing library`() {
  }
}
