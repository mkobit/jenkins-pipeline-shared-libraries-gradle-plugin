package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.testkit.runner.buildWith
import org.assertj.core.api.Assertions.anyOf
import org.assertj.core.api.Assertions.assertThat
import com.mkobit.gradle.test.assertj.GradleAssertions.assertThat
import org.assertj.core.api.Assertions.allOf
import org.assertj.core.api.Assertions.not
import org.gradle.api.artifacts.Configuration
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testsupport.GradleProject
import testsupport.GradleVersions
import testsupport.Integration
import testsupport.IntelliJSupport
import testsupport.NotImplementedYet
import testsupport.SampleCandidate
import testsupport.condition
import testsupport.softlyAssert
import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments.of as arguments

@Integration
@GradleVersions
internal class IntegrationTestSourceIntegrationTest {

  @TestTemplate
  internal fun `can execute dependencies task`(@GradleProject gradleRunner: GradleRunner) {
    gradleRunner.buildWith(arguments = listOf("dependencies"))
  }

  @TestTemplate
  internal fun `integrationTest compile classpath does not contain any HPI or JPI artifacts`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("--quiet", "showResolvedArtifacts"))

    softlyAssert {
      assertThat(buildResult.output.trim().split(System.lineSeparator())).isNotEmpty.allSatisfy {
        assertThat(it)
          .doesNotEndWith("@hpi")
          .doesNotEndWith("@jpi")
          .endsWith("@jar")
      }
    }
  }

  @TestTemplate
  internal fun `integrationTest runtimeOnly configuration contains only HPI, JPI, and WAR artifacts`(@GradleProject gradleRunner: GradleRunner) {
    // Can't test the runtimeOnly directly because we can't
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("--quiet", "showResolvedArtifacts"))

    softlyAssert {
      assertThat(buildResult.output.trim().split(System.lineSeparator())).isNotEmpty.allSatisfy {
        val hpi = condition<String>(".hpi extension") { it.endsWith("@hpi") }
        val jpi = condition<String>(".jpi extension") { it.endsWith("@jpi") }
        val war = condition<String>(".war extension") { it.endsWith("@war") }
        val jenkinsWar = condition<String>("Jenkins WAR group and module") { it.contains("org.jenkins-ci.main:jenkins-war") }
        assertThat(it).has(anyOf(allOf(war, jenkinsWar), hpi, jpi))
      }
    }
  }

  @TestTemplate
  internal fun `JenkinsPipelineUnit is not available in the integrationTest classpath`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("--quiet", "showResolvedArtifacts"))

    softlyAssert {
      assertThat(buildResult.output.trim().split(System.lineSeparator())).isNotEmpty.allSatisfy {
        val group = condition<String>("Jenkins Pipeline Unit group: com.lesfurets") { it.contains("com.lesfurets:") }
        val module = condition<String>("Jenkins Pipeline Unit module: jenkins-pipeline-unit") { it.contains(":jenkins-pipeline-unit:") }
        assertThat(it).has(not(anyOf(group, module)))
      }
    }
  }

  @TestTemplate
  internal fun `can compile integration test sources that use Jenkins libraries`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("compileIntegrationTestGroovy", "-i"))

    val task = buildResult.task(":compileIntegrationTestGroovy")
    assertThat(task?.outcome)
      .describedAs("integrationTestCompileGroovy task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @TestTemplate
  internal fun `can use @JenkinsRule in integration tests`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("integrationTest", "-i"))

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @TestTemplate
  internal fun `WorkflowJob can be created and executed in integration tests`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("integrationTest", "-i"))

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @TestTemplate
  internal fun `can set up Global Pipeline Library and use them in an integration test`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("integrationTest", "-i"))

    val task = buildResult.task(":integrationTest")
    assertThat(task)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isSuccess
  }

  @TestTemplate
  internal fun `run integrationTest repeatedly then tasks are up-to-date`(@GradleProject gradleRunner: GradleRunner) {
    // Exclude classes to not test main source compilation
    gradleRunner.buildWith(arguments = listOf("integrationTest", "-x", "classes", "-i")).let { buildResult ->
      assertThat(buildResult)
        .tasksWithOutcomeSatisfy(TaskOutcome.SUCCESS) {
          assertThat(it).isNotEmpty
        }.tasksWithOutcomeSatisfy(TaskOutcome.UP_TO_DATE) {
          assertThat(it).isEmpty()
        }
    }

    gradleRunner.buildWith(arguments = listOf("integrationTest", "-x", "classes", "-i")).let { buildResult ->
      assertThat(buildResult)
        .tasksWithOutcomeSatisfy(TaskOutcome.SUCCESS) {
          assertThat(it).isEmpty()
        }.tasksWithOutcomeSatisfy(TaskOutcome.UP_TO_DATE) {
          assertThat(it).isNotEmpty
        }
    }
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

  @TestTemplate
  internal fun `no configurations are resolved if no build tasks are executed`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("--quiet", "printConfigurationStates"))

    assertThat(buildResult.output.trim().split(System.lineSeparator())).allSatisfy {
      assertThat(it).endsWith(Configuration.State.UNRESOLVED.name)
    }
  }

  @TestTemplate
  internal fun `Groovy DSL extension configuration`(@GradleProject gradleRunner: GradleRunner) {
    gradleRunner.buildWith(arguments = listOf("-i"))
  }

  @Disabled("This example works in a local Gradle project, but not with Gradle Test Kit. See https://github.com/gradle/kotlin-dsl/issues/492")
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

  @TestTemplate
  internal fun `"check" lifecycle task executes "integrationTest"`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("check"))

    assertThat(buildResult)
      .hasTaskAtPath(":test")
      .hasTaskAtPath(":integrationTest")
  }

  @TestTemplate
  internal fun `can use generated Groovy sources in integration tests`(@GradleProject gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.buildWith(arguments = listOf("check", "-i"))
    assertThat(buildResult)
      .hasTaskSuccessAtPath(":compileIntegrationTestGroovy")
  }

  @NotImplementedYet
  @Test
  internal fun `integration tests are executed when build lifecycle task is executed`() {
  }

  @NotImplementedYet
  @Test
  internal fun `failing integration tests fail build tasks`() {
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
